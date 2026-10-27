package love.yinlin.repository.nativecompose

import java.io.File
import java.nio.channels.FileChannel
import java.nio.channels.OverlappingFileLockException
import java.nio.channels.FileLock
import java.nio.file.StandardOpenOption

/** Owns only generated files recorded by the source preparer, preserving arbitrary local edits. */
internal class SourceWorkspace(private val vendor: File) {
    fun reusable(destination: File, record: File, identity: SourceObject): Boolean {
        checkedSourcePath(vendor, destination)
        checkedSourcePath(vendor, record)
        if (!record.isFile || !destination.isDirectory) return false
        val previous = record.sourceJson()
        if (identity.any { (key, value) -> previous[key] != value }) return false
        val hashes = previous["fileHashes"] as? Map<*, *> ?: return false
        if (hashes.isEmpty() || hashes.values.any { it !is String }) return false
        val files = hashes.keys.map { sourceRelativePath(it as String) }.toSet()
        check((sourceFiles(destination) - files).isEmpty()) { "Unrecognized source files preserved in $destination" }
        var complete = true
        for ((relative, expected) in hashes) {
            val file = checkedSourcePath(vendor, destination.resolve(relative as String))
            if (!file.isFile) complete = false
            else check(file.sourceHash() == expected) { "Local source edit preserved: $file" }
        }
        return complete
    }

    private fun sourceFiles(destination: File): Set<String> = destination.walkTopDown().onEnter { it.name !in setOf("build", ".git", ".gradle", ".kotlin", "dependencies") }.filter {
        it.isFile && it.extension in setOf("kt", "cc", "cpp", "h", "hh", "def") && "/src/" in it.invariantSeparatorsPath
    }.map { it.relativeTo(destination).invariantSeparatorsPath }.toSet()

    fun <T> locked(directory: File, action: () -> T): T {
        directory.mkdirs()
        checkedSourcePath(vendor, directory.resolve(".prepare.lock"))
        return FileChannel.open(directory.resolve(".prepare.lock").toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
            val deadline = System.nanoTime() + java.util.concurrent.TimeUnit.MINUTES.toNanos(5)
            var acquired: FileLock? = null
            while (acquired == null) {
                acquired = try { channel.tryLock() } catch (_: OverlappingFileLockException) { null }
                check(acquired != null || System.nanoTime() < deadline) { "Timed out waiting for Native source preparation: $directory" }
                if (acquired == null) Thread.sleep(100)
            }
            acquired.use { action() }
        }
    }

    fun synchronize(staging: File, destination: File, record: File, identity: SourceObject, preservedDirectories: Set<String> = emptySet()): Map<String, String> {
        checkedSourcePath(vendor, destination)
        checkedSourcePath(vendor, record)
        preservedDirectories.forEach { sourceRelativePath(it) }
        val files = staging.walkTopDown().filter { it.isFile }.associate { it.relativeTo(staging).invariantSeparatorsPath to it }
        check(files.keys.none { relative -> preservedDirectories.any { relative == it || relative.startsWith("$it/") } }) { "Prepared sources overlap an independently managed SDK: $destination" }
        val hashes = files.mapValues { it.value.sourceHash() }
        val previous = if (record.isFile) record.sourceJson() else emptyMap()
        val known = (previous["fileHashes"] as? Map<*, *>)?.mapKeys { it.key as String }
        val legacy = previous["files"] as? List<*>
        if (previous.isEmpty() && destination.exists()) {
            // CI can restore the SDK cache while the separately keyed source snapshot misses.
            for (existing in destination.walkTopDown().drop(1)) {
                checkedSourcePath(vendor, existing)
                val relative = existing.relativeTo(destination).invariantSeparatorsPath
                check(preservedDirectories.any { relative == it || relative.startsWith("$it/") || (existing.isDirectory && it.startsWith("$relative/")) }) { "Unrecognized source workspace preserved: $existing" }
            }
        }
        val oldFiles = known?.keys ?: legacy?.map { it as String }?.toSet() ?: emptySet()
        val currentSources = sourceFiles(destination)
        check((currentSources - oldFiles - files.keys).isEmpty()) { "Unrecognized source files preserved in $destination: ${currentSources - oldFiles - files.keys}" }
        for (relative in oldFiles) {
            sourceRelativePath(relative)
            val file = checkedSourcePath(vendor, destination.resolve(relative))
            val current = if (file.isFile) file.sourceHash() else null
            val expected = known?.get(relative)
            val allowed = when (expected) {
                is List<*> -> expected.toSet()
                is String -> setOf(expected)
                else -> setOf(hashes[relative])
            }
            check(current == null || current in allowed) { "Local source edit preserved: $file" }
        }
        for ((relative, source) in files) {
            val target = checkedSourcePath(vendor, destination.resolve(relative))
            check(relative in oldFiles || !target.exists() || (target.isFile && target.sourceHash() == hashes.getValue(relative))) { "Unrecognized generated source preserved: $target" }
        }
        // A resumable plan accepts only the previous or complete new contents.
        val pending = linkedMapOf<String, Any?>()
        for (relative in oldFiles + hashes.keys) {
            val old = known?.get(relative)
            val values = if (old is List<*>) old else listOfNotNull(old)
            pending[relative] = (values + listOfNotNull(hashes[relative])).distinct()
        }
        writeSource(vendor, record, sourceBytes(identity + ("files" to (oldFiles + hashes.keys).sorted()) + ("fileHashes" to pending)))
        for (relative in oldFiles - hashes.keys) {
            val removed = checkedSourcePath(vendor, destination.resolve(relative))
            check(!removed.exists() || removed.delete()) { "Cannot remove generated source: $removed" }
        }
        for ((relative, source) in files) writeSource(vendor, destination.resolve(relative), source.readBytes())
        writeSource(vendor, record, sourceBytes(identity + ("files" to hashes.keys.sorted()) + ("fileHashes" to hashes)))
        return hashes
    }
}
