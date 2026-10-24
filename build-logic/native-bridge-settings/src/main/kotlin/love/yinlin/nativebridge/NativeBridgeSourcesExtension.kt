package love.yinlin.nativebridge

import org.gradle.api.initialization.Settings

/** Registers a public Gradle project selector in the context of its included build. */
class NativeBridgeSourcesExtension(private val settings: Settings) {
    fun include(rule: String, directory: Any, module: String, buildName: String = rule, projectPath: String = ":") {
        settings.includeBuild(directory) {
            name = buildName
            dependencySubstitution {
                val substitutions = this
                val selector = substitutions.project(projectPath)
                settings.gradle.extensions.extraProperties.set("love.yinlin.nativeBridge.source.$rule", selector)
                substitutions.substitute(substitutions.module(module)).using(selector)
            }
        }
    }
}
