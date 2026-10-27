package love.yinlin.nativebridge

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal object ComposeResourceGeneration {
    fun configure(project: Project, kotlin: KotlinMultiplatformExtension, bridge: NativeBridgeExtension) {
        val compose = project.extensions.findByName("compose") as? ExtensionAware ?: return
        val resources = compose.extensions.findByType(ResourcesExtension::class.java) ?: return
        if (resources.generateResClass != ResourcesExtension.ResourceClassGeneration.Auto) return
        val rule = bridge.redirects.findByName("compose-components-resources") ?: return
        if (!rule.module.isPresent) return
        val coordinate = rule.module.get().split(':')
        val commonMain = kotlin.sourceSets.findByName("commonMain") ?: return
        val dependencies = project.configurations.getByName(commonMain.implementationConfigurationName).allDependencies + project.configurations.getByName(commonMain.apiConfigurationName).allDependencies
        if (dependencies.any { it.group == coordinate[0] && it.name == coordinate[1] }) resources.generateResClass = ResourcesExtension.ResourceClassGeneration.Always
    }
}
