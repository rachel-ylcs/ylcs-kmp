package love.yinlin.gradle.task

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.DigestInputStream
import java.security.MessageDigest
import java.time.Duration
import java.util.Base64
import java.util.Properties
import java.util.zip.GZIPInputStream

/** Fetches an SDK package by its NuGet identity and verifies it against the official catalog. */
@DisableCachingByDefault(because = "The local package is verified before every use")
abstract class PrepareNuGetPackageTask : DefaultTask() {
    @get:Input
    abstract val packageId: Property<String>

    @get:Input
    abstract val packageVersion: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    abstract val offline: Property<Boolean>

    @get:Internal
    val archive get() = outputDirectory.file("package.nupkg")

    init {
        group = "native setup"
        offline.convention(project.gradle.startParameter.isOffline)
        outputDirectory.convention(project.layout.buildDirectory.dir(packageVersion.map { "nativeNuGet/$name/${version(it)}" }))
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun prepare() {
        val id = packageId.get().lowercase()
        val version = version(packageVersion.get())
        require(Regex("[a-z0-9]+(?:[._-][a-z0-9]+)*").matches(id)) { "Invalid NuGet package ID: $id" }
        val packageFile = archive.get().asFile
        val receipt = packageFile.parentFile.resolve("receipt.properties")
        if (receipt.isFile && packageFile.isFile) {
            val saved = Properties().apply { receipt.inputStream().use(::load) }
            if (saved.getProperty("id") == id && saved.getProperty("version") == version &&
                saved.getProperty("size")?.toLongOrNull() == packageFile.length() && packageFile.sha512() == saved.getProperty("sha512")) return
        }
        check(!offline.get()) { "No verified NuGet package cached for $id $version in offline mode." }

        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).followRedirects(HttpClient.Redirect.NORMAL).build().use { client ->
            val resources = client.json("https://api.nuget.org/v3/index.json")["resources"] as List<*>
            val registration = resources.service("RegistrationsBaseUrl/3.6.0")
            val packages = resources.service("PackageBaseAddress/3.0.0")
            val leaf = client.json("$registration$id/$version.json")
            val metadata = client.json(leaf["catalogEntry"] as String)
            check((metadata["id"] as String).equals(id, ignoreCase = true) && (metadata["version"] as String).equals(version, ignoreCase = true)) { "NuGet catalog identity mismatch for $id $version" }
            check(metadata["packageHashAlgorithm"] == "SHA512") { "Unsupported NuGet hash algorithm for $id $version" }
            val expected = metadata["packageHash"] as String
            check(Base64.getDecoder().decode(expected).size == 64) { "Invalid NuGet package digest for $id $version" }
            val size = (metadata["packageSize"] as Number).toLong()
            check(size in 1 .. 1024L * 1024 * 1024) { "Invalid NuGet package size for $id $version: $size" }
            val url = "$packages$id/$version/$id.$version.nupkg"
            packageFile.parentFile.mkdirs()
            val staging = Files.createTempFile(packageFile.parentFile.toPath(), "download-", ".tmp").toFile()
            try {
                client.get(url, Duration.ofMinutes(5)).use { input ->
                    staging.outputStream().use { output ->
                        check(input.transferBounded(output, size) == size) { "Truncated NuGet package for $id $version" }
                    }
                }
                check(staging.sha512() == expected) { "NuGet SHA-512 mismatch for $id $version" }
                Files.move(staging.toPath(), packageFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                val pending = Files.createTempFile(packageFile.parentFile.toPath(), "receipt-", ".tmp").toFile()
                try {
                    pending.writeText("id=$id\nversion=$version\nsize=$size\nsha512=$expected\n")
                    Files.move(pending.toPath(), receipt.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                }
                finally { pending.delete() }
            }
            finally { staging.delete() }
        }
    }

    private fun version(value: String): String = value.lowercase().also {
        require(Regex("[0-9]+(?:[.][0-9]+){2,3}(?:-[a-z0-9]+(?:[.-][a-z0-9]+)*)?").matches(it)) { "Invalid NuGet package version: $value" }
    }

    private fun List<*>.service(type: String): String {
        val item = filterIsInstance<Map<*, *>>().firstOrNull { it["@type"] == type } ?: error("Missing NuGet service: $type")
        return (item["@id"] as String).trimEnd('/') + "/"
    }

    private fun HttpClient.get(url: String, timeout: Duration): InputStream {
        val uri = URI(url)
        check(uri.scheme == "https" && uri.host == "api.nuget.org") { "Unexpected NuGet service URL: $url" }
        val response = send(HttpRequest.newBuilder(uri).timeout(timeout).GET().build(), HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() != 200 || response.uri().scheme != "https" || response.uri().host != "api.nuget.org") {
            response.body().close()
            error("NuGet download returned HTTP ${response.statusCode()}: $url")
        }
        return response.body()
    }

    private fun HttpClient.json(url: String): Map<*, *> = get(url, Duration.ofSeconds(30)).buffered().use { input ->
        input.mark(2)
        val gzip = input.read() == 0x1f && input.read() == 0x8b
        input.reset()
        val limit = 8 * 1024 * 1024
        val bytes = if (gzip) GZIPInputStream(input).use { it.readNBytes(limit + 1) } else input.readNBytes(limit + 1)
        check(bytes.size <= limit) { "Oversized NuGet metadata: $url" }
        JsonSlurper().parseText(bytes.toString(Charsets.UTF_8)) as Map<*, *>
    }

    private fun File.sha512(): String {
        val digest = MessageDigest.getInstance("SHA-512")
        DigestInputStream(inputStream(), digest).use { it.transferTo(OutputStream.nullOutputStream()) }
        return Base64.getEncoder().encodeToString(digest.digest())
    }

    private fun InputStream.transferBounded(output: OutputStream, expected: Long): Long {
        val buffer = ByteArray(64 * 1024)
        var count = 0L
        while (true) {
            val size = read(buffer)
            if (size < 0) return count
            count += size
            check(count <= expected) { "NuGet package exceeds its published size" }
            output.write(buffer, 0, size)
        }
    }
}
