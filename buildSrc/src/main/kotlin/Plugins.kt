import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.PluginDependenciesSpecScope
import org.gradle.plugin.use.PluginDependency

private val PrebuildPlugins = mutableListOf<String>()

fun PluginDependenciesSpecScope.install(vararg plugins: Provider<PluginDependency>) {
    for (plugin in plugins) {
        val id = plugin.get().pluginId
        if (id in PrebuildPlugins) id(id)
        else alias(plugin)
    }
}

fun PluginDependenciesSpecScope.install(prebuildPlugins: List<Provider<PluginDependency>>, vararg plugins: Provider<PluginDependency>) {
    PrebuildPlugins += prebuildPlugins.map { it.get().pluginId }
    for (plugin in plugins) {
        val id = plugin.get().pluginId
        if (id in PrebuildPlugins) id(id).apply(false)
        else alias(plugin).apply(false)
    }
}