package love.yinlin.nativebridge

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ComponentSelector
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

internal object NativePortDependencyAudit {
    fun configure(project: Project, kotlin: KotlinMultiplatformExtension, bridge: NativeBridgeExtension, source: (String) -> ComponentSelector?) {
        project.tasks.register("verifyNativePortDependencies") {
            group = "verification"
            description = "Resolve Native compile graphs and reject official artifacts for owned ports"
            doLast {
                for (target in kotlin.targets.withType(KotlinNativeTarget::class.java)) {
                    val rules = bridge.redirects.filter { target.konanTarget.name in it.targets.get() }
                    val configuration = project.configurations.getByName(target.compilations.getByName("main").compileDependencyConfigurationName)
                    val dependencies = configuration.incoming.resolutionResult.allDependencies
                    dependencies.filterIsInstance<UnresolvedDependencyResult>().firstOrNull()?.let { throw it.failure }
                    val verified = linkedSetOf<String>()
                    for (dependency in dependencies.filterIsInstance<ResolvedDependencyResult>()) {
                        val requested = dependency.requested as? ModuleComponentSelector
                        val selected = dependency.selected.moduleVersion
                        val rule = rules.singleOrNull {
                            requested != null && matches(it, requested.group, requested.module, target.konanTarget.name) ||
                                selected != null && matches(it, selected.group, selected.name, target.konanTarget.name)
                        } ?: continue
                        verify(rule, target.konanTarget.name, dependency.selected, source(rule.name))
                        verified.add(rule.name)
                    }
                    project.logger.lifecycle("PASS Native port dependencies ${target.name} (${target.konanTarget.name}): ${verified.size} owned ports; ${verified.sorted().joinToString()}")
                }
            }
        }
    }

    private fun matches(rule: NativeDependencyRule, group: String, name: String, target: String): Boolean {
        if (rule.matchesUpstream(group, name, target)) return true
        if (!rule.module.isPresent) return false
        val coordinate = rule.module.get().split(':')
        return group == coordinate[0] && name in setOf(coordinate[1], "${coordinate[1]}-${target.replace("_", "")}")
    }

    private fun verify(rule: NativeDependencyRule, target: String, selected: ResolvedComponentResult, source: ComponentSelector?) {
        val id = selected.id
        if (source != null) {
            require(source.matchesStrictly(id)) { "${rule.name}/$target must use the registered source project ${source.displayName}; selected ${id.displayName}" }
        }
        else if (rule.projectPath.isPresent) {
            require(id is ProjectComponentIdentifier && id.projectPath == rule.projectPath.get()) { "${rule.name}/$target must use ${rule.projectPath.get()}; selected ${id.displayName}" }
        }
        else {
            val coordinate = rule.module.get().split(':')
            require(id is ModuleComponentIdentifier && id.group == coordinate[0] && id.module in setOf(coordinate[1], "${coordinate[1]}-${target.replace("_", "")}") && id.version == coordinate[2]) {
                "${rule.name}/$target must use the owned Maven artifact ${rule.module.get()}; selected ${id.displayName}"
            }
        }
    }
}
