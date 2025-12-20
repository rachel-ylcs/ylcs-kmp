import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.PluginDependenciesSpecScope
import org.gradle.plugin.use.PluginDependency

private val PrebuildPlugins = mutableListOf<String>()

fun PluginDependenciesSpecScope.install(vararg plugins: Provider<PluginDependency>) {
    for (plugin in plugins) {
        val pluginId = plugin.get().pluginId
        if (pluginId in PrebuildPlugins) id(pluginId)
        else alias(plugin)
    }
}

fun PluginDependenciesSpecScope.install(
    prebuildPlugins: List<Provider<PluginDependency>>,
    customPlugins: List<Provider<PluginDependency>>,
) {
    for (plugin in prebuildPlugins) {
        val pluginId = plugin.get().pluginId
        PrebuildPlugins += pluginId
        id(pluginId).apply(false)
    }
    for (plugin in customPlugins) {
        alias(plugin).apply(false)
    }
}