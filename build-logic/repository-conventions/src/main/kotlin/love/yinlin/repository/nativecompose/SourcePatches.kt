package love.yinlin.repository.nativecompose

import java.io.File

internal class SourcePatches(private val nativeDirectory: File) {
    private val configuration = nativeDirectory.resolve("upstream/patches/manifest.json").sourceJson()
    val manifest = configuration.filterKeys { it != "schema" }.mapValues { (name, value) ->
        if (name == "maven") value.sourceObject().mapValues { (_, patch) -> describe(patch.sourceObject()) }
        else describe(value.sourceObject())
    }

    private fun describe(specification: SourceObject): SourceObject = specification + ("sha256" to file(specification).sourceHash())

    fun file(specification: SourceObject): File {
        val name = specification["file"] as String
        sourceRelativePath(name)
        val file = nativeDirectory.resolve("upstream/patches/$name")
        check(file.isFile) { "Missing source patch: $file" }
        check(specification["sha256"] == null || file.sourceHash() == specification["sha256"]) { "Source patch changed during preparation: $file" }
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
}
