package love.yinlin.nativebridge

import org.gradle.api.initialization.Settings

/** Registers a public Gradle project selector in the context of its included build. */
class NativeBridgeSourcesExtension(private val settings: Settings) {
    fun include(rule: String, directory: Any, module: String, buildName: String = rule, projectPath: String = ":") {
        include(directory, buildName, mapOf(rule to (module to projectPath)))
    }

    /** Maps multiple libraries from one included build; each value is module to project path. */
    fun include(directory: Any, buildName: String, projects: Map<String, Pair<String, String>>) {
        settings.includeBuild(directory) {
            name = buildName
            dependencySubstitution {
                val substitutions = this
                projects.forEach { (rule, source) ->
                    val selector = substitutions.project(source.second)
                    settings.gradle.extensions.extraProperties.set("love.yinlin.nativeBridge.source.$rule", selector)
                    substitutions.substitute(substitutions.module(source.first)).using(selector)
                }
            }
        }
    }
}
