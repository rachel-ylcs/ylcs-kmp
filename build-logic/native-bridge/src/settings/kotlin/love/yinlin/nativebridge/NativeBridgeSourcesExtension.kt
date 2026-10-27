package love.yinlin.nativebridge

import org.gradle.api.initialization.Settings
import java.io.File

class NativeBridgeSourcesExtension(private val settings: Settings) {
    fun include(rule: String, directory: Any, module: String, buildName: String = rule, projectPath: String = ":") {
        include(directory, buildName, mapOf(rule to (module to projectPath)))
    }

    fun include(directory: Any, buildName: String, projects: Map<String, Pair<String, String>>) {
        include(directory, buildName, projects, emptyMap())
    }

    fun include(directory: Any, buildName: String, projects: Map<String, Pair<String, String>>, adaptationDirectories: Map<String, File>) {
        projects.forEach { (rule, source) ->
            settings.gradle.extensions.extraProperties.set("love.yinlin.nativeBridge.sourceInfo.$rule", mapOf(
                "build" to buildName,
                "project" to source.second,
                "adaptation" to (adaptationDirectories[rule]?.resolve("build.gradle.kts")?.invariantSeparatorsPath ?: ""),
            ))
        }
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
