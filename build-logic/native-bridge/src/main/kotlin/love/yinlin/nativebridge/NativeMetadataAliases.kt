package love.yinlin.nativebridge

import org.gradle.api.Project
import org.gradle.api.Action
import org.gradle.api.attributes.Attribute
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

internal object NativeMetadataAliases {
    fun configure(project: Project, kotlin: KotlinMultiplatformExtension, bridge: NativeBridgeExtension) {
        val participants = kotlin.targets.filter { it.platformType != KotlinPlatformType.common }
        val mixed = bridge.redirects.any { rule ->
            participants.any { it is KotlinNativeTarget && it.konanTarget.name in rule.targets.get() } &&
                participants.any { it !is KotlinNativeTarget || it.konanTarget.name !in rule.targets.get() }
        }
        if (!mixed) return
        val aliases = buildMap {
            for (rule in bridge.redirects) {
                if (!rule.module.isPresent) continue
                val port = rule.module.get().split(':').take(2).joinToString(":")
                val upstream = rule.upstream.get()
                put(port, upstream)
                for (target in rule.targets.get()) {
                    val suffix = target.replace("_", "")
                    put("$port-$suffix", "$upstream-$suffix")
                }
            }
        }
        project.dependencies.components.all(Action {
            val component = this
            val identifier = "${component.id.group}:${component.id.name}"
            if (identifier !in aliases) return@Action
            val root = bridge.redirects.any { it.module.isPresent && it.module.get().substringBeforeLast(':') == identifier }
            component.allVariants {
                // KMP root Native variants are available-at links. Applying a dependency
                // rule to those links can create an external-variant cycle in Gradle.
                if (root && attributes.getAttribute(KotlinPlatformType.attribute) != KotlinPlatformType.common) return@allVariants
                withDependencies {
                    val dependencies = this
                    for (dependency in dependencies.toList()) {
                        val upstream = aliases["${dependency.group}:${dependency.name}"] ?: continue
                        // Classifier requests are runtime payloads, not Kotlin metadata edges.
                        if (dependency.artifactSelectors.isNotEmpty()) continue
                        val constraint = dependency.versionConstraint
                        dependencies.remove(dependency)
                        dependencies.add("$upstream:${constraint.requiredVersion}") {
                            val replacement = this
                            replacement.version {
                                if (constraint.strictVersion.isNotEmpty()) strictly(constraint.strictVersion)
                                if (constraint.preferredVersion.isNotEmpty()) prefer(constraint.preferredVersion)
                                if (constraint.rejectedVersions.isNotEmpty()) reject(*constraint.rejectedVersions.toTypedArray())
                                branch = constraint.branch
                            }
                            dependency.reason?.let(replacement::because)
                            if (dependency.isEndorsingStrictVersions) replacement.endorseStrictVersions()
                            replacement.attributes {
                                for (key in dependency.attributes.keySet()) {
                                    @Suppress("UNCHECKED_CAST")
                                    val attribute = key as Attribute<Any>
                                    dependency.attributes.getAttribute(attribute)?.let { attribute(attribute, it) }
                                }
                            }
                        }
                    }
                }
            }
        })
    }
}
