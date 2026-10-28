package love.yinlin.repository.nativecompose

import java.io.File
import java.nio.file.Files

internal class SourcePatches(private val nativeDirectory: File) {
    val manifest = nativeDirectory.resolve("upstream/patches/manifest.json").sourceJson()

    fun file(specification: SourceObject): File {
        val name = specification["file"] as String
        sourceRelativePath(name)
        val file = nativeDirectory.resolve("upstream/patches/$name")
        check(file.isFile && file.sourceHash() == specification["sha256"]) { "Source patch changed without updating its manifest: $file" }
        return file
    }

    fun paths(specification: SourceObject): Set<String> {
        val result = linkedSetOf<String>()
        for (line in file(specification).readLines()) {
            if (!line.startsWith("diff --git ")) continue
            val match = Regex("^diff --git a/(.+) b/(.+)$").matchEntire(line) ?: error("Unsupported source patch path: $line")
            result.add(sourceRelativePath(match.groupValues[1]))
            result.add(sourceRelativePath(match.groupValues[2]))
        }
        check(result.isNotEmpty()) { "Source patch is empty" }
        return result
    }

    fun apply(directory: File, specification: SourceObject) {
        val patch = file(specification)
        val temporaryGit = !directory.resolve(".git").exists()
        if (temporaryGit) sourceGit(directory, "init", "-q")
        try {
            for (relative in paths(specification)) checkedSourcePath(directory, directory.resolve(relative))
            sourceGit(directory, "apply", "--check", "--whitespace=nowarn", patch.absolutePath)
            sourceGit(directory, "apply", "--whitespace=nowarn", patch.absolutePath)
        } finally {
            if (temporaryGit) {
                val git = checkedSourcePath(directory, directory.resolve(".git"))
                check(git.name == ".git" && git.parentFile.canonicalFile == directory.canonicalFile)
                check(git.deleteRecursively()) { "Cannot remove temporary patch metadata: $git" }
            }
        }
    }

    fun prepareGitPatch(workspace: File, record: File, commit: String, specification: SourceObject) {
        val staging = Files.createTempDirectory(nativeDirectory.resolve("vendor").toPath(), ".patch-plan-").toFile()
        try {
            val originals = linkedMapOf<String, String?>()
            for (relative in paths(specification) + SourceWarnings.trackedPaths(workspace)) {
                val source = checkedSourcePath(workspace, workspace.resolve(relative))
                originals[relative] = if (source.isFile) source.sourceHash() else null
                if (source.isFile) source.copyTo(staging.resolve(relative).apply { parentFile.mkdirs() })
            }
            apply(staging, specification)
            SourceWarnings.prepare(nativeDirectory.resolve("vendor"), staging)
            val hashes = originals.mapValues { (relative, original) ->
                val prepared = staging.resolve(relative).takeIf { it.isFile }?.sourceHash()
                mapOf("original" to original, "prepared" to prepared)
            }
            val vendor = nativeDirectory.resolve("vendor")
            writeSource(vendor, record, sourceBytes(mapOf("schema" to 4, "commit" to commit, "patchSha256" to specification["sha256"], "warningPolicy" to SourceWarnings.policy, "files" to hashes)))
            // Each recorded file accepts either its original or complete prepared contents,
            // including a preparation interrupted between two files.
            for (relative in hashes.keys) {
                val target = checkedSourcePath(workspace, workspace.resolve(relative))
                val prepared = staging.resolve(relative)
                if (prepared.isFile) writeSource(vendor, target, prepared.readBytes())
                else check(!target.exists() || target.delete()) { "Cannot remove patched source: $target" }
            }
        } finally {
            checkedSourcePath(nativeDirectory.resolve("vendor"), staging)
            check(staging.name.startsWith(".patch-plan-"))
            check(staging.deleteRecursively())
        }
    }
}
