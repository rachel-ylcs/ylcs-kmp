import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.PluginDependenciesSpecScope
import org.gradle.plugin.use.PluginDependency

/**
 * Preloads catalog plugin versions in the root with `apply = false`, or applies those plugins
 * in a module. Module requests inherit the preloaded implementations without repeating versions.
 *
 * This entry point deliberately uses the default package: Gradle compiles `plugins {}` separately
 * and does not retain the surrounding script's imports in that compilation stage.
 * Inlining also avoids loading a helper class from the plugin implementation's classloader while
 * Gradle is still collecting a child script's plugin requests.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun PluginDependenciesSpecScope.install(vararg plugins: Provider<PluginDependency>, apply: Boolean = true) {
    for (plugin in plugins) {
        if (apply) id(plugin.get().pluginId)
        else alias(plugin).apply(false)
    }
}
