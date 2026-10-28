package love.yinlin.nativebridge

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.GenerateProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

/** Keeps included-build dependency identities separate from the coordinates stored for Maven publication. */
internal object NativeSourceMetadata {
    fun configure(project: Project, kotlin: KotlinMultiplatformExtension, bridge: NativeBridgeExtension, usesSource: (String) -> Boolean) {
        val rules = bridge.redirects.filter { it.module.isPresent && (usesSource(it.name) || it.projectPath.isPresent) }
        val ports = rules.associate { rule ->
            rule.module.get().substringBeforeLast(':') to rule.upstream.get()
        }
        if (ports.isEmpty()) return

        val nativeTargets = rules.flatMap { it.targets.get() }.toSet()
        val roots = kotlin.sourceSets.mapNotNull { sourceSet ->
            val participants = kotlin.targets.filter { target ->
                target.platformType != KotlinPlatformType.common && target.compilations.findByName("main")?.allKotlinSourceSets?.contains(sourceSet) == true
            }
            if (participants.size < 2 || participants.none { it is KotlinNativeTarget && it.konanTarget.name in nativeTargets }) return@mapNotNull null
            project.configurations.getByName("${sourceSet.name}ResolvableDependenciesMetadata").incoming.resolutionResult.rootComponent
        }
        if (roots.isEmpty()) return

        val metadata = project.tasks.named("generateProjectStructureMetadata", GenerateProjectStructureMetadata::class.java)
        val aliases = project.provider { sourceAliases(roots.map { it.get() }, ports) }
        project.gradle.projectsEvaluated {
            // KGP installs the secondary variant after finalizing its compilations. Applications
            // without a consumable metadata variant need no source-metadata publication.
            val variant = project.configurations.getByName("metadataApiElements").outgoing.variants.findByName("kotlinProjectStructureMetadata") ?: return@projectsEvaluated
            val sourceMetadata = project.tasks.register("prepareNativeSourceMetadata", NormalizeNativeSourceMetadata::class.java) {
                group = "native setup"
                description = "Normalize project identities for Native included-build source metadata"
                inputFile.set(project.layout.file(metadata.map { it.resultFile }))
                moduleAliases.set(aliases)
                outputFile.set(project.layout.buildDirectory.file("kotlinProjectStructureMetadata/native-source/kotlin-project-structure-metadata.json"))
            }

            // KGP consumes this secondary variant for local source visibility. The publication JAR
            // continues to embed generateProjectStructureMetadata's original Maven coordinates.
            variant.artifacts.clear()
            variant.artifact(sourceMetadata.flatMap { it.outputFile }) {
                classifier = "psm-metadata"
                type = "kotlin-psm-metadata"
                extension = "json"
            }
        }
    }

    private fun sourceAliases(roots: List<ResolvedComponentResult>, ports: Map<String, String>): Map<String, String> {
        val projects = mutableSetOf<String>()
        val visited = mutableSetOf<org.gradle.api.artifacts.component.ComponentIdentifier>()
        val queue = ArrayDeque<ResolvedComponentResult>()
        queue.addAll(roots)
        while (queue.isNotEmpty()) {
            val component = queue.removeFirst()
            if (!visited.add(component.id)) continue
            if (component.id is ProjectComponentIdentifier) {
                component.moduleVersion?.let { projects.add("${it.group}:${it.name}") }
            }
            component.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach { queue.add(it.selected) }
        }
        // Only rewrite identities backed by an actual selected source project. Maven-only
        // dependencies and projects already using the publication identity are left intact.
        return ports.filter { (port, upstream) -> port != upstream && upstream in projects }
    }
}
