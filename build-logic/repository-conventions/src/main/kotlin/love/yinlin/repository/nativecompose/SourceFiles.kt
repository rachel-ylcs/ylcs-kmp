package love.yinlin.repository.nativecompose

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

internal typealias SourceObject = Map<String, Any?>

@Suppress("UNCHECKED_CAST")
internal fun Any?.sourceObject(): SourceObject = this as? Map<String, Any?> ?: error("Expected a source lock object")

internal fun File.sourceJson(): SourceObject {
    check(isFile && length() <= 32L * 1024 * 1024) { "Missing or oversized source record: $this" }
    return JsonSlurper().parse(this).sourceObject()
}

internal fun File.sourceHash(): String = inputStream().use { input ->
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(64 * 1024)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        digest.update(buffer, 0, count)
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}

internal fun bytesHash(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

internal fun sourceBytes(value: Any): ByteArray = (JsonOutput.prettyPrint(JsonOutput.toJson(value)) + "\n").toByteArray(Charsets.UTF_8)

internal fun checkedSourcePath(root: File, path: File): File {
    val base = root.canonicalFile.toPath()
    val target = path.canonicalFile.toPath()
    check(target.startsWith(base) && target != base && !Files.isSymbolicLink(path.toPath())) { "Generated path escapes its workspace: $path" }
    var parent = path.absoluteFile.toPath()
    while (!Files.exists(parent)) parent = parent.parent ?: error("Missing workspace parent: $path")
    if (root.exists()) check(parent.toRealPath().startsWith(root.toPath().toRealPath())) { "Generated path follows a link outside its workspace: $path" }
    return path
}

internal fun sourceRelativePath(relative: String): String {
    check(relative.isNotEmpty() && relative.none { it == '\u0000' || it == '\n' || it == '\r' } && !relative.startsWith('/') && '\\' !in relative && ':' !in relative && relative.split('/').none { it.isEmpty() || it == "." || it == ".." }) { "Unsafe source entry: $relative" }
    return relative
}

internal fun writeSource(root: File, file: File, bytes: ByteArray) {
    checkedSourcePath(root, file)
    if (file.isFile && file.readBytes().contentEquals(bytes)) return
    file.parentFile.mkdirs()
    val temporary = checkedSourcePath(root, file.resolveSibling("${file.name}.tmp"))
    temporary.writeBytes(bytes)
    Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
}

internal fun File.resolveSibling(name: String): File = parentFile.resolve(name)

internal fun sourceCommand(directory: File, arguments: List<String>, allowedExitCodes: Set<Int> = setOf(0)): String {
    val process = ProcessBuilder(arguments).directory(directory).redirectErrorStream(true).start()
    val bytes = ByteArrayOutputStream()
    var readFailure: Throwable? = null
    val reader = thread(name = "native-source-output", isDaemon = true) {
        try {
            process.inputStream.use { stream ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    check(bytes.size() + count <= 8 * 1024 * 1024) { "Source command output exceeds its size limit" }
                    bytes.write(buffer, 0, count)
                }
            }
        } catch (error: Throwable) { readFailure = error }
    }
    if (!process.waitFor(5, TimeUnit.MINUTES)) {
        process.destroyForcibly()
        reader.join(5000)
        error("Native source command timed out: ${arguments.first()}")
    }
    reader.join(5000)
    check(!reader.isAlive) { "Source command output did not close" }
    readFailure?.let { throw it }
    val output = bytes.toString(Charsets.UTF_8)
    check(process.exitValue() in allowedExitCodes) { "Native source command failed (${process.exitValue()}): ${arguments.joinToString(" ")}\n$output" }
    return output
}

internal fun sourceGit(directory: File, vararg arguments: String): String = sourceCommand(directory,
    listOf("git", "-c", "safe.directory=${directory.canonicalFile.invariantSeparatorsPath}", "-c", "core.fsmonitor=false", "-c", "core.longpaths=true", "-c", "core.autocrlf=false", "-C", directory.absolutePath) + arguments)

internal fun downloadSource(url: String, destination: File, expectedHash: String, offline: Boolean, maximum: Long) {
    check(Regex("[a-f0-9]{64}").matches(expectedHash)) { "Missing immutable source hash" }
    if (!destination.isFile) {
        check(!offline) { "Offline source archive is missing: $destination" }
        val uri = URI(url)
        check(uri.scheme == "https" && uri.host in setOf("codeload.github.com", "github.com", "repo.maven.apache.org")) { "Unaudited source download: $url" }
        destination.parentFile.mkdirs()
        val temporary = destination.resolveSibling("${destination.name}.part")
        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).followRedirects(HttpClient.Redirect.NORMAL).build()
        val response = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(5)).GET().build(), HttpResponse.BodyHandlers.ofInputStream())
        response.body().use { input ->
            check(response.statusCode() == 200) { "Source download returned HTTP ${response.statusCode()}: $url" }
            temporary.outputStream().use { output ->
                var total = 0L
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    check(total <= maximum) { "Source archive exceeds its size limit: $url" }
                    output.write(buffer, 0, count)
                }
            }
        }
        check(temporary.sourceHash() == expectedHash) { "Downloaded source SHA-256 mismatch: $temporary" }
        Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
    check(destination.length() <= maximum && destination.sourceHash() == expectedHash) { "Cached source SHA-256 mismatch: $destination" }
}
