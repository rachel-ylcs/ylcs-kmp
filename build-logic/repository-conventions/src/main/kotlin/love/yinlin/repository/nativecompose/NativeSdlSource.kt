package love.yinlin.repository.nativecompose

import java.io.File
import java.nio.file.Files

internal object NativeSdlSource {
    private const val upstream = "#define SDL_DYNAMIC_API 1"
    private const val prepared = "#define SDL_DYNAMIC_API 0 /* Rachel: direct calls for the static Compose window host. */"

    fun disableDynamicApi(directory: File) {
        // SDL deliberately requires a source edit; a compiler definition triggers an error in this header.
        val header = directory.resolve("src/dynapi/SDL_dynapi.h")
        val text = header.readText()
        val enabled = text.lineSequence().count { it == upstream }
        val disabled = text.lineSequence().count { it == prepared }
        check(enabled == 1 && disabled == 0 || enabled == 0 && disabled == 1) { "Review SDL's dynamic API switch after the upstream update: $header" }
        if (disabled == 1) return
        header.writeText(Regex("(?m)^${Regex.escape(upstream)}(?=\\r?$)").replace(text, prepared))
    }

    fun prepareDropNegotiation(nativeDirectory: File, directory: File, archive: File, sourceSha256: String) {
        val patches = SourcePatches(nativeDirectory)
        val specification = patches.manifest["sdl"].sourceObject()
        patches.file(specification)
        check(specification["sourceSha256"] == sourceSha256) { "Review the SDL drop negotiation patch after updating SDL" }
        val record = directory.resolve(".rachel-drop-patch.json")
        val previous = record.takeIf { it.isFile }?.sourceJson()
        val previousFiles = previous?.get("files")?.sourceObject().orEmpty()
        for ((relative, value) in previousFiles) {
            val file = checkedSourcePath(directory, directory.resolve(sourceRelativePath(relative)))
            val hashes = value.sourceObject()
            val actual = file.takeIf { it.isFile }?.sourceHash()
            check(actual == hashes["original"] || actual == hashes["prepared"] || actual == hashes["previous"]) { "Local SDL edits preserved; review before applying the drop patch: $file" }
        }
        if (previous?.get("patchSha256") == specification["sha256"] && previousFiles.all { (relative, value) ->
            directory.resolve(relative).takeIf { it.isFile }?.sourceHash() == value.sourceObject()["prepared"]
        }) return

        check(archive.isFile && archive.sourceHash() == sourceSha256) { "Missing or modified pinned SDL source archive: $archive" }
        val paths = patches.paths(specification) + previousFiles.keys
        val staging = Files.createTempDirectory(directory.parentFile.toPath(), ".sdl-drop-patch-").toFile()
        try {
            val originals = SourceArchive.extract(archive, staging, true) { it.takeIf(paths::contains) }
            for (relative in paths - previousFiles.keys) {
                val file = checkedSourcePath(directory, directory.resolve(relative))
                check(file.takeIf { it.isFile }?.sourceHash() == originals[relative]) { "Local SDL edits preserved: $file" }
            }
            patches.apply(staging, specification)
            val hashes = paths.associateWith { relative ->
                mapOf("original" to originals[relative], "previous" to directory.resolve(relative).takeIf { it.isFile }?.sourceHash(), "prepared" to staging.resolve(relative).takeIf { it.isFile }?.sourceHash())
            }
            // Record the complete plan first so preparation interrupted between files can resume.
            writeSource(directory, record, sourceBytes(mapOf("sourceSha256" to sourceSha256, "patchSha256" to specification["sha256"], "files" to hashes)))
            for (relative in paths) {
                val prepared = staging.resolve(relative)
                val destination = checkedSourcePath(directory, directory.resolve(relative))
                if (prepared.isFile) writeSource(directory, destination, prepared.readBytes())
                else check(!destination.exists() || destination.delete()) { "Cannot remove obsolete SDL patch source: $destination" }
            }
            val completed = hashes.mapValues { (_, values) -> values.filterKeys { it != "previous" } }
            writeSource(directory, record, sourceBytes(mapOf("sourceSha256" to sourceSha256, "patchSha256" to specification["sha256"], "files" to completed)))
        }
        finally {
            checkedSourcePath(directory.parentFile, staging)
            check(staging.name.startsWith(".sdl-drop-patch-"))
            check(staging.deleteRecursively())
        }
    }
}
