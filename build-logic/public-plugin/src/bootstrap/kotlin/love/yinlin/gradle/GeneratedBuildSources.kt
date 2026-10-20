package love.yinlin.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

object GeneratedBuildSources {
    private const val ELEMENTS = "rachelGeneratedBuildSourcesElements"
    private const val IDE_SOURCES = "rachelIdeGeneratedBuildSources"
    private const val SOURCES_USAGE = "rachel-generated-build-sources"
    private const val PREPARE_MODEL = "prepareKotlinBuildScriptModel"

    fun elements(project: Project): Configuration = project.configurations.maybeCreate(ELEMENTS).apply {
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, SOURCES_USAGE))
    }

    fun publish(project: Project, source: Provider<RegularFile>) {
        project.artifacts.add(elements(project).name, source)
        project.tasks.matching { it.name == PREPARE_MODEL }.configureEach {
            dependsOn(source)
        }
    }

    fun aggregate(project: Project, modulePaths: List<String>) {
        val sources = project.configurations.create(IDE_SOURCES) {
            isCanBeConsumed = false
            isCanBeResolved = true
            isTransitive = false
            attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, SOURCES_USAGE))
        }
        for (path in modulePaths) {
            sources.dependencies.add(project.dependencies.project(mapOf("path" to path, "configuration" to ELEMENTS)))
        }
        project.tasks.matching { it.name == PREPARE_MODEL }.configureEach {
            dependsOn(sources)
        }
    }
}
