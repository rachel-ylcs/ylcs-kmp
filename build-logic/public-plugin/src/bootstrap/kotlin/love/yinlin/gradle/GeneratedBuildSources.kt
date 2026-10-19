package love.yinlin.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

/** Connects generated sources to IDE preparation through ordinary project artifacts. */
object GeneratedBuildSources {
    private const val Elements = "rachelGeneratedBuildSourcesElements"
    private const val IdeSources = "rachelIdeGeneratedBuildSources"
    private const val SourcesUsage = "rachel-generated-build-sources"
    private const val PrepareModel = "prepareKotlinBuildScriptModel"

    /** An empty variant also lets a repository aggregate projects without generators. */
    fun elements(project: Project): Configuration = project.configurations.maybeCreate(Elements).apply {
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, SourcesUsage))
    }

    /** The provider retains the producing task without realizing it during configuration. */
    fun publish(project: Project, source: Provider<RegularFile>) {
        project.artifacts.add(elements(project).name, source)
        project.tasks.matching { it.name == PrepareModel }.configureEach {
            dependsOn(source)
        }
    }

    /** Called on the root project with descriptor paths collected during settings evaluation. */
    fun aggregate(project: Project, modulePaths: List<String>) {
        val sources = project.configurations.create(IdeSources) {
            isCanBeConsumed = false
            isCanBeResolved = true
            isTransitive = false
            attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, SourcesUsage))
        }
        for (path in modulePaths) {
            sources.dependencies.add(project.dependencies.project(mapOf("path" to path, "configuration" to Elements)))
        }
        project.tasks.matching { it.name == PrepareModel }.configureEach {
            dependsOn(sources)
        }
    }
}
