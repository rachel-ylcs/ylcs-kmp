package love.yinlin.repository.nativecompose

import java.io.File

internal class NativeCiInputs(private val repository: File) {
    private val accessors: Set<String> = listOf("native-compose/adaptation", "native-compose/gradle", "native-compose/verification", "build-logic/native-bridge", "build-logic/public-plugin/build.gradle.kts").flatMap { relative ->
        repository.resolve(relative).walkTopDown().onEnter { it.name !in setOf("build", "vendor", ".gradle", ".kotlin") }.filter {
            it.isFile && (it.name.endsWith(".gradle.kts") || it.name.endsWith(".gradle.kts.template"))
        }.flatMap { Regex("\\blibs\\.([A-Za-z0-9_.]+)").findAll(it.readText()).map { match -> match.groupValues[1] } }.toList()
    }.toSet()

    private fun referenced(alias: String, prefix: String = ""): Boolean {
        val accessor = prefix + alias.replace('-', '.').replace('_', '.')
        return accessors.any { it == accessor || it.startsWith("$accessor.") }
    }

    fun identity(catalog: NativeSourceCatalog): SourceObject {
        val versions = catalog.versions.filterKeys { it in setOf("kotlin", "cmp", "skiko", "sdl", "agp", "dokka", "coroutines", "mavenPublish") || (!it.startsWith("rachel") && referenced(it, "versions.")) }.toSortedMap()
        val libraries = catalog.libraries.filter { (alias, value) ->
            referenced(alias) || alias.startsWith("native-") || value.first.startsWith("org.jetbrains.compose.") || value.first.startsWith("org.jetbrains.skiko:") ||
                value.first in setOf("org.jetbrains:annotations", "org.jetbrains.kotlinx:kotlinx-coroutines-core", "org.jetbrains.kotlinx:atomicfu")
        }.toSortedMap().mapValues { (_, value) -> listOf(value.first, value.second) }
        val plugins = catalog.plugins.filterKeys { referenced(it, "plugins.") }.toSortedMap().mapValues { (alias, value) ->
            if (alias in setOf("rachel", "repository", "nativeBridge", "nativeBridgeSettings")) listOf(value.first)
            else listOf(value.first, value.second)
        }
        return mapOf("versions" to versions, "libraries" to libraries, "plugins" to plugins)
    }

    companion object {
        @JvmStatic
        fun main(arguments: Array<String>) {
            require(arguments.size in 2 .. 3) { "Expected repository, output file and optional base catalog" }
            val repository = File(arguments[0]).canonicalFile
            val inputs = NativeCiInputs(repository)
            val catalog = NativeSourceCatalog(repository)
            val kotlin = catalog.versions.getValue("kotlin")
            check(Regex("[0-9]+\\.[0-9]+\\.[0-9]+[A-Za-z0-9.-]*").matches(kotlin)) { "Invalid Kotlin version for a CI cache key" }
            val identity = inputs.identity(catalog)
            val base = arguments.getOrNull(2)?.takeIf { it.isNotBlank() }?.let { File(it) }
            val changed = base == null || inputs.identity(NativeSourceCatalog(repository, base)) != identity
            val output = File(arguments[1])
            output.parentFile.mkdirs()
            output.writeText("kotlin-version=$kotlin\nnative-inputs=${bytesHash(sourceBytes(identity))}\ncatalog-changed=$changed\n")
        }
    }
}
