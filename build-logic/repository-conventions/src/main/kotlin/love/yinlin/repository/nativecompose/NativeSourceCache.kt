package love.yinlin.repository.nativecompose

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Snapshots only prepared source inputs; SDKs and compiler outputs have separate CI caches. */
class NativeSourceCache(directory: File) {
    private val nativeDirectory = directory.canonicalFile
    private val vendor = nativeDirectory.resolve("vendor")
    private val archive = vendor.resolve("cache/prepared-sources.zip")
    private val sdlVersion = nativeDirectory.resolve("upstream/sdl.json").takeIf { it.isFile }?.sourceJson()?.get("version") as? String
    private val roots = listOf("skiko/workspace", "compose/workspace", "resources/workspace", "maven/workspace", "maven/states", "icu/workspace") + listOfNotNull(sdlVersion?.let {
        check(Regex("[0-9]+\\.[0-9]+\\.[0-9]+").matches(it)) { "Invalid cached SDL source version" }
        "sdl-source-$it"
    })
    private val markers = setOf("skiko/state.json", "compose/state.json", "compose/source-edits.json", "compose/checkout.txt", "resources/source-edits.json", "resources/checkout.txt")
    private val excluded = setOf("build", ".gradle", ".kotlin", "dependencies", "node_modules", "local.properties", ".prepare.lock", "__pycache__")

    init {
        vendor.mkdirs()
        check(nativeDirectory.name == "native-compose" && vendor.canonicalFile.parentFile == nativeDirectory && !Files.isSymbolicLink(vendor.toPath())) { "Source cache escapes native-compose" }
    }

    private fun allowed(name: String): Boolean {
        sourceRelativePath(name)
        return name.split('/').none { it in excluded } && (name in markers || roots.any { name.startsWith("$it/") })
    }

    fun create() {
        vendor.mkdirs()
        val files = linkedMapOf<String, File>()
        for (root in roots) {
            val directory = checkedSourcePath(vendor, vendor.resolve(root))
            check(directory.isDirectory) { "Prepare upstream sources before caching: $directory" }
            for (file in directory.walkTopDown().onEnter { candidate ->
                checkedSourcePath(vendor, candidate)
                candidate.name !in excluded
            }.filter { it.isFile }) {
                val relative = file.relativeTo(vendor).invariantSeparatorsPath
                if (allowed(relative)) files[relative] = checkedSourcePath(vendor, file)
            }
        }
        for (relative in markers) vendor.resolve(relative).takeIf { it.isFile }?.let { files[relative] = checkedSourcePath(vendor, it) }
        SourceArchive.snapshot(files, checkedSourcePath(vendor, archive))
        println("Cached ${files.size} prepared source inputs; SDKs and build outputs excluded")
    }

    fun restore() {
        if (!archive.isFile) {
            println("No prepared source snapshot; source preparation will populate it")
            return
        }
        checkedSourcePath(vendor, archive)
        val staging = Files.createTempDirectory(vendor.toPath(), ".source-cache-").toFile()
        try {
            val files = SourceArchive.extract(archive, staging, false) { relative ->
                check(allowed(relative)) { "Unexpected source cache entry: $relative" }
                relative
            }
            check(files.isNotEmpty()) { "Empty prepared source cache" }
            // Validate the complete snapshot and all destinations before writing any file.
            for (relative in files.keys) check(!checkedSourcePath(vendor, vendor.resolve(relative)).exists()) { "Restore source caches only into a fresh workspace: $relative" }
            for (relative in files.keys) {
                val destination = checkedSourcePath(vendor, vendor.resolve(relative))
                destination.parentFile.mkdirs()
                Files.copy(staging.resolve(relative).toPath(), destination.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
            }
            println("Restored ${files.size} prepared source inputs; synchronization will validate them against the locks")
        } finally {
            checkedSourcePath(vendor, staging)
            check(staging.name.startsWith(".source-cache-"))
            check(staging.deleteRecursively())
        }
    }
}
