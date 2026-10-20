import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.PluginDependenciesSpecScope
import org.gradle.plugin.use.PluginDependency

@Suppress("NOTHING_TO_INLINE")
inline fun PluginDependenciesSpecScope.install(vararg plugins: Provider<PluginDependency>, apply: Boolean = true) {
    for (plugin in plugins) {
        if (apply) id(plugin.get().pluginId)
        else alias(plugin).apply(false)
    }
}
