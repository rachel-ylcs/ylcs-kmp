package love.yinlin.mod

import androidx.compose.runtime.Stable
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.io.readTo
import love.yinlin.data.Data
import love.yinlin.data.mod.ModInfo
import love.yinlin.data.mod.ModMetadata
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicResource
import love.yinlin.data.music.MusicResourceType
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.toJsonString
import love.yinlin.platform.Coroutines
import kotlin.random.Random

object ModFactory {
    private const val MAGIC = 1211
    private const val INTERVAL = 1024 * 64
    const val VERSION = 4

    @Stable
    class Merge(
        private val mediaPaths: List<Path>,
        private val sink: Sink,
        private val info: ModInfo = ModInfo()
    ) {
        companion object {
            private fun Sink.writeLengthString(str: String) {
                val bytes = str.encodeToByteArray()
                writeInt(bytes.size)
                write(bytes)
            }
        }

        constructor(mediaPath: Path, sink: Sink) : this(listOf(mediaPath), sink)

        private suspend fun Sink.writeMetadata() {
            Coroutines.io {
                writeInt(MAGIC) // 写文件头
                writeInt(VERSION) // 写版本号
                writeInt(mediaPaths.size) // 写媒体数
                writeLengthString(info.toJsonString()) // 写额外信息
            }
        }

        private suspend fun Sink.writeResource(resourcePath: Path, resource: MusicResource) {
            Coroutines.io {
                writeInt(resource.id) // 写资源类型
                writeLengthString(resource.name) // 写资源名称
                // 写资源数据
                val resLength = SystemFileSystem.metadataOrNull(resourcePath)!!.size.toInt()
                require(resLength > 0) { "资源长度非法 $resourcePath Length: $resLength" }
                val times = resLength / INTERVAL
                val remain = resLength - times * INTERVAL
                writeInt(resLength) // 写资源长度
                SystemFileSystem.source(resourcePath).buffered().use { source ->
                    repeat(times) {
                        source.readTo(this@writeResource, INTERVAL.toLong())
                        writeByte(Random.nextInt(127).toByte())
                    }
                    if (remain > 0) source.readTo(this@writeResource, remain.toLong())
                }
            }
        }

        private suspend fun Sink.writeMedia(mediaPath: Path, filter: List<MusicResourceType>) {
            Coroutines.io {
                writeLengthString(mediaPath.name) // 写媒体ID
                val resourcePaths = SystemFileSystem.list(mediaPath).filter { path ->
                    val resource = MusicResource.fromString(path.name)
                    require(resource != null) { "资源名非法 ${path.name}" }
                    filter.find { it.id == resource.id } == null
                }
                writeInt(resourcePaths.size) // 写资源数
                for (resourcePath in resourcePaths) {
                    val resource = MusicResource.fromString(resourcePath.name)
                    require(resource != null) { "资源名非法 ${resourcePath.name}" }
                    writeResource(resourcePath, resource)
                }
            }
        }

        suspend fun process(
            filter: List<MusicResourceType> = listOf(),
            onProcess: (Int, Int, String) -> Unit
        ): Data<Unit> = try {
            sink.writeMetadata()
            for ((index, mediaPath) in mediaPaths.withIndex()) {
                sink.writeMedia(mediaPath, filter)
                Coroutines.main {
                    onProcess(index, mediaPaths.size, mediaPath.name)
                }
            }
            Data.Success(Unit)
        } catch (e: Throwable) {
            Data.Failure(throwable = e)
        }
    }

    @Stable
    abstract class BaseRelease(protected val source: Source) {
        companion object {
            fun Source.readLengthString(): String {
                val length = readInt()
                require(length >= 0) { "MOD文件已破坏" }
                val bytes = readByteArray(length)
                return bytes.decodeToString()
            }
        }

        protected suspend fun Source.readMetadata(): ModMetadata = Coroutines.io {
            val magic = readInt() // 读文件头
            require(magic == MAGIC) { "非MOD文件" }
            val version = readInt() // 读版本
            require(version == VERSION) { "不兼容的MOD版本 APP: $VERSION, MOD: $version" }
            val mediaNum = readInt() // 读媒体数
            require(mediaNum >= 0) { "MOD文件已破坏" }
            val info = readLengthString().parseJsonValue<ModInfo>()
            require(info != null) { "MOD文件已破坏" }
            ModMetadata(version, mediaNum, info)
        }
    }

    @Stable
    class Release(
        source: Source,
        private val savePath: Path
    ): BaseRelease(source) {
        @Stable
        data class ReleaseResult(
            val metadata: ModMetadata,
            val medias: List<String>
        )

        private suspend fun Source.readResource(mediaPath: Path) = Coroutines.io {
            val resType = readInt() // 读资源类型
            val resName = readLengthString() // 读资源名称
            val resource = MusicResource(resType, resName)
            val resLength = readInt() // 读资源长度
            require(resLength > 0) { "资源长度非法 Length: $resLength" }
            val times = resLength / INTERVAL
            val remain = resLength - times * INTERVAL
            // 读资源数据
            SystemFileSystem.sink(Path(mediaPath, resource.toString())).buffered().use { sink ->
                repeat(times) {
                    readTo(sink, INTERVAL.toLong())
                    readByte()
                }
                if (remain > 0) readTo(sink, remain.toLong())
            }
        }

        private suspend fun Source.readMedia(): String = Coroutines.io {
            val id = readLengthString() // 读媒体ID
            val mediaPath = Path(savePath, id)
            SystemFileSystem.createDirectories(mediaPath)
            val resourceNum = readInt() // 读资源数
            repeat(resourceNum) {
                readResource(mediaPath)
            }
            id
        }

        suspend fun process(onProcess: (Int, Int, String) -> Unit): Data<ReleaseResult> = try {
            val metadata = source.readMetadata()
            val ids = mutableListOf<String>()
            repeat(metadata.mediaNum) { index ->
                val id = source.readMedia()
                ids += id
                Coroutines.main {
                    onProcess(index, metadata.mediaNum, id)
                }
            }
            Data.Success(ReleaseResult(metadata, ids))
        } catch (e: Throwable) {
            Data.Failure(throwable = e)
        }
    }

    @Stable
    class Preview(source: Source): BaseRelease(source) {
        @Stable
        data class ResourceItem(
            val resource: MusicResource,
            val length: Int
        )

        @Stable
        data class MediaItem(
            val id: String,
            val config: MusicInfo?,
            val resources: List<ResourceItem>
        )

        @Stable
        data class PreviewResult(
            val metadata: ModMetadata,
            val medias: List<MediaItem>
        )

        private suspend fun Source.previewResource(): Pair<ResourceItem, MusicInfo?> = Coroutines.io {
            val resType = readInt() // 读资源类型
            val resName = readLengthString() // 读资源名称
            val resource = MusicResource(resType, resName)
            val resLength = readInt() // 读资源长度
            require(resLength > 0) { "资源长度非法 Length: $resLength" }
            val times = resLength / INTERVAL
            val config = if (resource.type == MusicResourceType.Config) { // 读取媒体配置
                val bytes = ByteArray(resLength)
                readTo(bytes)
                bytes.decodeToString().parseJsonValue<MusicInfo>()
            }
            else { // 跳过资源数据
                skip((resLength + times).toLong())
                null
            }
            ResourceItem(resource, resLength) to config
        }

        private suspend fun Source.previewMedia(): MediaItem = Coroutines.io {
            val id = readLengthString() // 读媒体ID
            val resourceNum = readInt() // 读资源数
            val resources = mutableListOf<ResourceItem>()
            var mainConfig: MusicInfo? = null
            repeat(resourceNum) {
                val (resource, config) = previewResource()
                resources += resource
                if (config != null) mainConfig = config
            }
            MediaItem(id, mainConfig, resources)
        }

        suspend fun process(): Data<PreviewResult> = try {
            val metadata = source.readMetadata()
            val medias = mutableListOf<MediaItem>()
            repeat(metadata.mediaNum) { index ->
                medias += source.previewMedia()
            }
            Data.Success(PreviewResult(metadata, medias))
        } catch (e: Throwable) {
            Data.Failure(throwable = e)
        }
    }
}