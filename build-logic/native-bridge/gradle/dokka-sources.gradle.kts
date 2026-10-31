import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.Sync
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask

val adaptationDirectory = project.buildFile.parentFile.canonicalFile.toPath()
val nativeDirectory = adaptationDirectory.parent.parent

afterEvaluate {
    val dokka = extensions.getByType<DokkaExtension>()
    with(dokka) {
        dokkaSourceSets.configureEach {
            if (suppress.get()) return@configureEach
            val sourceSetName = name
            val originalRoots = project.objects.fileCollection().from(sourceRoots.from.toList())
            val sources = tasks.register<Sync>("prepare${sourceSetName.replaceFirstChar(Char::uppercase)}DokkaSources") {
                into(layout.buildDirectory.dir("dokka-sources/$sourceSetName"))
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
                fileTree(task.destinationDir.resolve("upstream")) { include("**/*.kt", "**/*.java") }
            })
        }
    }
    tasks.withType<DokkaGenerateTask>().configureEach {
        inputs.files(provider { dokka.dokkaSourceSets.filter { !it.suppress.get() }.map { it.sourceRoots } })
            .withPropertyName("nativeDocumentationAnalysisSources")
            .withPathSensitivity(PathSensitivity.RELATIVE)
    }
}
