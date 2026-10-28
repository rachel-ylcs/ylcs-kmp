package love.yinlin.nativebridge

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Produces the source-consumption view without modifying Kotlin's publication metadata. */
@CacheableTask
abstract class NormalizeNativeSourceMetadata : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFile: RegularFileProperty

    @get:Input
    abstract val moduleAliases: MapProperty<String, String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun normalize() {
        val input = inputFile.get().asFile.readBytes()
        val aliases = moduleAliases.get()
        val document = JsonSlurper().parse(input) as? Map<*, *> ?: error("Invalid Kotlin project structure metadata")
        val structure = document["projectStructure"] as? Map<*, *> ?: error("Missing Kotlin project structure")
        val sourceSets = structure["sourceSets"] as? List<*> ?: error("Missing Kotlin source sets")
        var changed = false
        val normalized = sourceSets.map { value ->
            val sourceSet = value as? Map<*, *> ?: error("Invalid Kotlin source set")
            val dependencies = sourceSet["moduleDependency"] as? List<*> ?: error("Missing source set module dependencies")
            val modules = dependencies.flatMap { value ->
                val module = value as? String ?: error("Invalid source set module dependency")
                val sourceIdentity = aliases[module]
                if (sourceIdentity != null && sourceIdentity != module) {
                    changed = true
                    // KGP matches publication coordinates inside one build and project group/name
                    // across included builds. Both names refer to the same selected source project.
                    listOf(module, sourceIdentity)
                }
                else listOf(module)
            }.distinct()
            sourceSet + ("moduleDependency" to modules)
        }
        val output = if (changed) {
            (JsonOutput.prettyPrint(JsonOutput.toJson(document + ("projectStructure" to (structure + ("sourceSets" to normalized))))) + "\n").toByteArray(Charsets.UTF_8)
        }
        else input
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeBytes(output)
        }
    }
}
