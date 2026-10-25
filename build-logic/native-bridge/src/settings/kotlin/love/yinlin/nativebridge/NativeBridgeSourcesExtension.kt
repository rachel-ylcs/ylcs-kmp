package love.yinlin.nativebridge

import org.gradle.api.initialization.Settings
import java.io.File

/** Registers a public Gradle project selector in the context of its included build. */
class NativeBridgeSourcesExtension(private val settings: Settings) {
    fun include(rule: String, directory: Any, module: String, buildName: String = rule, projectPath: String = ":") {
        include(directory, buildName, mapOf(rule to (module to projectPath)))
    }

    /** Maps multiple libraries from one included build; each value is module to project path. */
    fun include(directory: Any, buildName: String, projects: Map<String, Pair<String, String>>) {
        include(directory, buildName, projects, emptyMap())
    }

    /** Adds maintained source locations for nativeBridgeReport without changing project selection. */
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
