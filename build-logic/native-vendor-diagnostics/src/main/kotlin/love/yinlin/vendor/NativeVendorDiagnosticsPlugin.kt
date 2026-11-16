package love.yinlin.vendor

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class NativeVendorDiagnosticsPlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) = Unit

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = "love.yinlin.native-vendor-diagnostics"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact("love.yinlin.native", "native-vendor-diagnostics")

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.project
        // Both source builds have their root three directories below this generation.
        val generation = project.rootDir.resolve("../../..").canonicalFile
        return project.providers.provider { listOf(SubpluginOption("generation", generation.path)) }
    }
}
