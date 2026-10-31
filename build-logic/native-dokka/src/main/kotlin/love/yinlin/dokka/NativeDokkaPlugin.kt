package love.yinlin.dokka

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask

class NativeDokkaPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("org.jetbrains.dokka")
        val adaptationDirectory = project.buildFile.parentFile.canonicalFile.toPath()
        val nativeDirectory = adaptationDirectory.parent.parent

        project.afterEvaluate {
            val dokka = project.extensions.getByType<DokkaExtension>()
            dokka.dokkaSourceSets.all {
                if (suppress.get()) return@all
                val sourceSetName = name
                val originalRoots = project.objects.fileCollection().from(sourceRoots.from.toList())
                val sources = project.tasks.register<Sync>("prepare${sourceSetName.replaceFirstChar(Char::uppercase)}DokkaSources") {
                    into(project.layout.buildDirectory.dir("dokka-sources/$sourceSetName"))
                    includeEmptyDirs = false
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    from(originalRoots.asFileTree) {
                        include("**/*.kt", "**/*.java")
                        eachFile {
                            val original = file.canonicalFile.toPath()
                            require(original.startsWith(nativeDirectory)) { "Native documentation source is outside $nativeDirectory: $original" }
                            val category = if (original.startsWith(adaptationDirectory)) "adaptation" else "upstream"
                            path = "$category/${nativeDirectory.relativize(original).toString().replace('\\', '/')}"
                        }
                    }
                }
                sourceRoots.setFrom(sources.map { it.destinationDir })
                suppressedFiles.setFrom(sources.map { task ->
                    project.fileTree(task.destinationDir.resolve("upstream")) { include("**/*.kt", "**/*.java") }
                })
            }
            project.tasks.withType<DokkaGenerateTask>().configureEach {
                inputs.files(project.provider { dokka.dokkaSourceSets.filter { !it.suppress.get() }.map { it.sourceRoots } })
                    .withPropertyName("nativeDocumentationAnalysisSources")
                    .withPathSensitivity(PathSensitivity.RELATIVE)
            }
        }
    }
}
