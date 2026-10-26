package love.yinlin.repository.nativecompose

import java.io.File

/** Reads the repository's one-line version catalog entries used by source-build profiles. */
internal class NativeSourceCatalog(private val repository: File) {
    val file = repository.resolve("libs.versions.toml")
    private val text = file.readText()
    private val sections = Regex("(?m)^\\[([^]]+)]\\s*$").findAll(text).toList()
    private fun section(name: String): String {
        val index = sections.indexOfFirst { it.groupValues[1] == name }
        check(index >= 0) { "Missing [$name] in $file" }
        return text.substring(sections[index].range.last + 1, sections.getOrNull(index + 1)?.range?.first ?: text.length)
    }
    val versions: Map<String, String> = Regex("(?m)^([A-Za-z0-9_.-]+)\\s*=\\s*\"([^\"]+)\"\\s*$").findAll(section("versions")).associate { it.groupValues[1] to it.groupValues[2] }
    val libraries: Map<String, Pair<String, String>> = Regex("(?m)^([A-Za-z0-9_.-]+)\\s*=\\s*\\{([^\r\n]+)}\\s*$").findAll(section("libraries")).mapNotNull { match ->
        val fields = Regex("([A-Za-z0-9_.-]+)\\s*=\\s*\"([^\"]+)\"").findAll(match.groupValues[2]).associate { it.groupValues[1] to it.groupValues[2] }
        val module = fields["module"] ?: fields["group"]?.let { group -> fields["name"]?.let { "$group:$it" } } ?: return@mapNotNull null
        val version = fields["version.ref"]?.let { versions.getValue(it) } ?: fields["version"] ?: return@mapNotNull null
        match.groupValues[1] to (module to version)
    }.toMap()
    val java: Int = Regex("\\bjvm\\s*\\{\\s*version\\s*=\\s*(\\d+)").find(repository.resolve("settings.gradle.kts").readText())?.groupValues?.get(1)?.toInt()
        ?: error("Cannot read repository JVM toolchain version")

    fun alignUpstreamCatalog(target: File) {
        var content = target.readText()
        val upstreamVersions = Regex("(?m)^([A-Za-z0-9_.-]+)\\s*=\\s*\"[^\"]+\"[ \\t]*$")
        val byModule = linkedMapOf<String, String>()
        for ((_, identity) in libraries) {
            val previous = byModule.put(identity.first, identity.second)
            check(previous == null || previous == identity.second) { "Conflicting repository versions for ${identity.first}" }
        }
        val overrides = mutableMapOf("kotlin" to versions.getValue("kotlin"), "coroutines" to versions.getValue("coroutines"), "androidGradlePlugin" to versions.getValue("agp"), "dokka" to versions.getValue("dokka"))
        for (line in content.lineSequence()) {
            val module = Regex("module\\s*=\\s*\"([^\"]+)\"").find(line)?.groupValues?.get(1) ?: continue
            val reference = Regex("version\\.ref\\s*=\\s*\"([^\"]+)\"").find(line)?.groupValues?.get(1) ?: continue
            byModule[module]?.let { version ->
                check(overrides[reference] == null || overrides[reference] == version) { "Conflicting upstream profile: $reference" }
                overrides[reference] = version
            }
        }
        for ((key, value) in overrides) {
            val matches = upstreamVersions.findAll(content).filter { it.groupValues[1] == key }.toList()
            check(matches.size == 1) { "Upstream catalog changed: $key" }
            content = content.replaceRange(matches.single().range, "$key = \"$value\"")
        }
        target.writeText(content)
    }

    fun copyWrappers(directory: File) {
        val wrappers = directory.walkTopDown().onEnter { it.name !in setOf("build", ".gradle", ".git", "dependencies") }.filter { it.name == "gradle-wrapper.properties" && it.parentFile.name == "wrapper" && it.parentFile.parentFile.name == "gradle" }.toList()
        for (wrapper in wrappers) {
            val build = wrapper.parentFile.parentFile.parentFile
            for (relative in listOf("gradle/wrapper/gradle-wrapper.properties", "gradle/wrapper/gradle-wrapper.jar", "gradlew", "gradlew.bat")) {
                val source = repository.resolve(relative)
                check(source.isFile) { "Missing repository Wrapper: $relative" }
                source.copyTo(build.resolve(relative).apply { parentFile.mkdirs() }, overwrite = true)
            }
            if (File.separatorChar != '\\') check(build.resolve("gradlew").setExecutable(true)) { "Cannot preserve Wrapper executable permission" }
        }
    }
}
