package love.yinlin.compose.image

import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import love.yinlin.compose.image.cache.ImageDiskCache
import love.yinlin.compose.image.cache.ImageMemoryCache
import love.yinlin.compose.image.data.*
import love.yinlin.compose.image.decoder.ImageDecodeException
import love.yinlin.compose.image.decoder.decodePlatformImageFile
import love.yinlin.compose.image.decoder.platformAnimation
import love.yinlin.compose.image.input.*
import love.yinlin.concurrent.Lock
import love.yinlin.concurrent.Mutex
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.ioContext
import love.yinlin.coroutines.cpuContext
import love.yinlin.extension.catching
import love.yinlin.extension.catchingNull
import love.yinlin.extension.cleaning
import love.yinlin.extension.throwing
import love.yinlin.foundation.PlatformContext
import love.yinlin.fs.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

internal class ImageLoader(
    private val client: HttpClient,
    val components: ImageComponents,
    val configuration: ImageConfiguration,
    cacheDirectory: File? = null,
    private val context: PlatformContext,
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + ioContext)
    private val lock = Lock()
    private val maintenance = Mutex()
    private val memory = ImageMemoryCache(configuration.maxMemoryCacheBytes)
    private val disk = cacheDirectory?.takeIf { configuration.maxDiskCacheBytes > 0L }?.let {
        ImageDiskCache(it, configuration.maxDiskCacheBytes, configuration.maxSourceBytes)
    }
    private val flights = mutableMapOf<String, ImageFlight<ImageInputData>>()
    private val outputs = mutableMapOf<String, ImageFlight<ImageResult>>()
    private val operations = Semaphore(6)
    private val downloads = Semaphore(6)
    private val decodes = Semaphore(2)
    private var generation = 0L
    private var closed = false

    val cacheGeneration: Long get() = lock.synchronized { generation }
    val memoryCacheSize: Long get() = lock.synchronized { memory.size }

    suspend fun load(request: ImageRequest, onProgress: ((ImageProgress) -> Unit)? = null): ImageResult {
        currentCoroutineContext().ensureActive()

        val input = components.input(request.source)
        if (input === ByteArrayImageInput && request.source is ImageSource.Bytes) {
            require(request.source.data.size in 1 .. configuration.maxSourceBytes) { "Image input exceeds the byte budget" }
        }

        val sourceKey = if (input === LocalFileImageInput || input === ResourceFileImageInput ||
            (input === WebImageInput && request.source is ImageSource.Network && (request.source.headers.isEmpty() || request.source.hasContentKey)) ||
            (input === ByteArrayImageInput && request.source is ImageSource.Bytes && request.source.hasContentKey)) {
            request.sourceKey(input.cacheKey(request.source))
        }
        else {
            Coroutines.cpu {
                request.sourceKey(input.cacheKey(request.source))
            }
        }

        val resultKey = request.resultKey(sourceKey)
        val cacheGroup = request.cacheGroup(sourceKey)
        val epoch = maintenance.with {
            lock.synchronized {
                check(!closed) { "ImageLoader is closed" }
                generation
            }
        }

        lock.synchronized { memory.get(resultKey, cacheGroup, request) }?.let { return it }

        val reportsProgress = input !== LocalFileImageInput && input !== ResourceFileImageInput && input !== ByteArrayImageInput
        val flightKey = "$resultKey:${request.diskCache}:$epoch"

        val flight = lock.synchronized {
            check(!closed && generation == epoch) { "Image cache was cleared" }

            outputs.getOrPut(flightKey) {
                val progress = MutableStateFlow(ImageProgress())

                ImageFlight(scope.async(cpuContext, start = CoroutineStart.LAZY) {
                    operations.withPermit {
                        decode(request, sourceKey, resultKey, epoch, if (reportsProgress) { { progress.value = it } } else null)
                    }
                }, progress)
            }.also { it.users++ }
        }

        return consumeFlight(flightKey, flight, outputs, if (reportsProgress) onProgress else null)
    }

    private suspend fun decode(request: ImageRequest, sourceKey: String, resultKey: String, epoch: Long, onProgress: ((ImageProgress) -> Unit)?): ImageResult {
        val cacheGroup = request.cacheGroup(sourceKey)
        lock.synchronized { memory.get(resultKey, cacheGroup, request) }?.let { return it }

        if (request.source is ImageSource.LocalFile && components.platformFilesAllowed && components.input(request.source) === LocalFileImageInput) {
            val decoded = decodes.withPermit {
                decodePlatformImageFile(request.source.file, request.decodeOptions(), configuration.maxSourceBytes)
            }
            if (decoded != null) {
                lock.synchronized {
                    if (generation == epoch && !closed) memory.put(resultKey, cacheGroup, decoded)
                }
                return decoded
            }
        }

        val data = if (request.source is ImageSource.Bytes && components.input(request.source) === ByteArrayImageInput) {
            ImageInputData(request.source.data, ImageOrigin.Local)
        } else acquireData(sourceKey, request, epoch, onProgress)

        currentCoroutineContext().ensureActive()

        return throwing(::ImageDecodeException) {
            decodes.withPermit {
                Coroutines.cpu {
                    val decoder = components.open(data.bytes, request.decodeOptions())

                    val decoded = cleaning({
                        require(decoder.size.width > 0 && decoder.size.height > 0 && decoder.size.width.toLong() * decoder.size.height <= configuration.maxDecodePixels) { "Decoder exceeded the pixel budget" }
                        require(decoder.durations.size in 1 .. 10000 && decoder.repeatCount >= -1) { "Invalid decoder frame metadata" }

                        val frame = decoder.frame(0)
                        require(frame.width == decoder.size.width && frame.height == decoder.size.height) { "Decoder returned an unexpected frame size" }

                        val animation = if (request.animated && decoder.durations.size > 1) ImageAnimationData(
                            bytes = data.bytes,
                            size = decoder.size,
                            durations = decoder.durations,
                            repeatCount = decoder.repeatCount,
                            request = request,
                            platform = decoder.platformAnimation(),
                        ) else null

                        ImageResult(frame, decoder.originalSize, data.origin, animation)
                    }, decoder::close)

                    lock.synchronized {
                        if (generation == epoch && !closed) memory.put(resultKey, cacheGroup, decoded)
                    }

                    decoded
                }
            }
        }
    }

    private suspend fun acquireData(key: String, request: ImageRequest, epoch: Long, onProgress: ((ImageProgress) -> Unit)?): ImageInputData {
        val flightKey = "$key:${request.diskCache}:$epoch"

        val flight = lock.synchronized {
            check(!closed && generation == epoch) { "Image cache was cleared" }

            flights.getOrPut(flightKey) {
                val progress = MutableStateFlow(ImageProgress())

                ImageFlight(scope.async(start = CoroutineStart.LAZY) {
                    downloads.withPermit {
                        fetch(key, request, progress)
                    }
                }, progress)
            }.also { it.users++ }
        }

        return consumeFlight(flightKey, flight, flights, onProgress)
    }

    private suspend fun <T> consumeFlight(key: String, flight: ImageFlight<T>, active: MutableMap<String, ImageFlight<T>>, onProgress: ((ImageProgress) -> Unit)?): T = cleaning({
        if (onProgress == null) flight.result.await()
        else {
            coroutineScope {
                val observer = launch(start = CoroutineStart.UNDISPATCHED) {
                    flight.progress.collect { onProgress(it) }
                }

                val result = cleaning({ flight.result.await() }) { observer.cancelAndJoin() }

                onProgress(flight.progress.value)
                result
            }
        }
    }) {
        lock.synchronized {
            flight.users--

            if (flight.users == 0) {
                if (active[key] === flight) active.remove(key)
                flight.result.cancel()
            }
        }
    }

    private suspend fun fetch(key: String, request: ImageRequest, progress: MutableStateFlow<ImageProgress>): ImageInputData {
        val input = components.input(request.source)

        val data = input.read(
            source = request.source,
            context = ImageInputContext(
                context = context,
                maxBytes = configuration.maxSourceBytes,
                onProgress = { progress.value = it },
                network =  { source ->
                    fetchNetwork(key, source, request.diskCache, progress)
                }
            )
        )

        require(data.bytes.size in 1 .. configuration.maxSourceBytes) { "Image input exceeds the byte budget" }
        return data
    }

    private suspend fun fetchNetwork(key: String, source: ImageSource.Network, useDisk: Boolean, progress: MutableStateFlow<ImageProgress>): ImageInputData {
        val cached = if (useDisk) catchingNull { disk?.read(key) } else null
        if (cached != null) return cached

        val result = client.prepareGet(source.url) {
            for ([key, value] in source.headers) {
                headers.append(key, value)
            }
        }.execute { response ->
            check(response.status.value in 200 .. 299) { "Image HTTP ${response.status.value}" }

            val total = response.contentLength()?.takeIf { it >= 0L }
            require(total == null || total <= configuration.maxSourceBytes) { "Image response exceeds the byte budget" }

            val channel = response.bodyAsChannel()

            currentCoroutineContext().ensureActive()

            val bytes = total?.takeIf { it > 0L }?.let { ByteArray(it.toInt()) }
            var offset = 0
            var extra: Buffer? = null
            val chunk = ByteArray(8192)

            progress.value = ImageProgress(0, total)
            var lastProgress = TimeSource.Monotonic.markNow()

            while (true) {
                currentCoroutineContext().ensureActive()
                val direct = bytes != null && offset < bytes.size
                val count = if (direct) channel.readAvailable(bytes, offset, minOf(bytes.size - offset, chunk.size)) else channel.readAvailable(chunk, 0, chunk.size)

                if (count < 0) break
                if (count == 0) continue

                if (direct) offset += count
                else {
                    require(offset.toLong() + (extra?.size ?: 0L) + count <= configuration.maxSourceBytes) { "Image response exceeds the byte budget" }

                    val buffer = extra ?: Buffer().also { extra = it }
                    buffer.write(chunk, 0, count)
                }

                if (lastProgress.elapsedNow() >= 100.milliseconds) {
                    progress.value = ImageProgress(offset.toLong() + (extra?.size ?: 0L), total)
                    lastProgress = TimeSource.Monotonic.markNow()
                }
            }

            val tail = extra
            val received = offset.toLong() + (tail?.size ?: 0L)
            require(received > 0L) { "Empty image response" }

            progress.value = ImageProgress(received, total)

            val result = when {
                bytes == null -> requireNotNull(tail).readByteArray()
                tail == null -> if (offset == bytes.size) bytes else bytes.copyOf(offset)
                else -> {
                    val result = bytes.copyOf(received.toInt())

                    while (!tail.exhausted()) {
                        offset += tail.readAtMostTo(result, offset, result.size)
                    }
                    result
                }
            }

            ImageInputData(result, ImageOrigin.Network)
        }

        if (useDisk) {
            catching { disk?.write(key, result) }
        }
        return result
    }

    suspend fun clearCache() = maintenance.with {
        lock.synchronized {
            generation++

            for (flight in outputs.values) flight.result.cancel()

            outputs.clear()

            for (flight in flights.values) flight.result.cancel()

            flights.clear()
            memory.clear()
        }

        Coroutines.wait {
            disk?.clear()
        }
    }

    override fun close() {
        lock.synchronized {
            closed = true
            memory.clear()
            scope.cancel()
            outputs.clear()
            flights.clear()
        }
    }
}
