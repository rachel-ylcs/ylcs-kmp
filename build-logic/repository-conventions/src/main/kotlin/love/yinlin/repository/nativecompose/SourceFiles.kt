package love.yinlin.repository.nativecompose

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

internal typealias SourceObject = Map<String, Any?>

@Suppress("UNCHECKED_CAST")
internal fun Any?.sourceObject(): SourceObject = this as? Map<String, Any?> ?: error("Expected a source metadata object")

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

internal fun sourceBytes(value: Any?): ByteArray = (JsonOutput.prettyPrint(JsonOutput.toJson(value)) + "\n").toByteArray(Charsets.UTF_8)

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
    val process = ProcessBuilder(arguments).directory(directory).start()
    val bytes = ByteArrayOutputStream()
    val diagnostics = ByteArrayOutputStream()
    val total = AtomicInteger()
    val readFailure = AtomicReference<Throwable?>()
    // Git metadata is read from stdout; warnings and tracing must not become part of a URL or hash.
    val readers = listOf(process.inputStream to bytes, process.errorStream to diagnostics).mapIndexed { index, (input, output) ->
        thread(name = "native-source-output-$index", isDaemon = true) {
            try {
                input.use { stream ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        check(total.addAndGet(count) <= 8 * 1024 * 1024) { "Source command output exceeds its size limit" }
                        output.write(buffer, 0, count)
                    }
                }
            } catch (error: Throwable) {
                readFailure.compareAndSet(null, error)
                process.destroyForcibly()
            }
        }
    }
    if (!process.waitFor(5, TimeUnit.MINUTES)) {
        process.destroyForcibly()
        readers.forEach { it.join(5000) }
        error("Native source command timed out: ${arguments.first()}")
    }
    readers.forEach { it.join(5000) }
    check(readers.none { it.isAlive }) { "Source command output did not close" }
    readFailure.get()?.let { throw it }
    val output = bytes.toString(Charsets.UTF_8)
    check(process.exitValue() in allowedExitCodes) { "Native source command failed (${process.exitValue()}): ${arguments.joinToString(" ")}\n$output\n${diagnostics.toString(Charsets.UTF_8)}" }
    return output
}

internal fun sourceGit(directory: File, vararg arguments: String): String = sourceCommand(directory, buildList {
    addAll(listOf("git", "-c", "safe.directory=${directory.canonicalFile.invariantSeparatorsPath}", "-c", "core.fsmonitor=false", "-c", "core.autocrlf=false", "-c", "core.eol=lf"))
    addAll(if (File.separatorChar == '\\') listOf("-c", "core.longpaths=true") else emptyList())
    addAll(listOf("-C", directory.absolutePath))
    addAll(arguments)
})
