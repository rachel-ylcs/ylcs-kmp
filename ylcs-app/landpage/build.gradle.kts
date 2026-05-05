import love.yinlin.task.spec.zip
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
    )
}

template(object : KotlinJsTemplate() {
    override fun KotlinJsSourceSetsScope.source() {
        jsMain.configure(commonMain) {
            lib(
                libs.kotlinx.html,
                projects.ylcsModule.core,
                projects.ylcsApp.cs,
            )
        }
    }

    override fun KotlinWebpackConfig.webpack() {
        devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
            port = C.host.webServerPort
            client?.overlay = false
            open = false
        }
    }

    override fun Project.actions() {
        val landpageRun by tasks.registering {
            dependsOn(tasks.named("jsBrowserDevelopmentRun"))
        }

        val landpagePublish by tasks.registering {
            dependsOn(tasks.named("jsBrowserDistribution"))

            doLast {
                val outputAppDir = C.root.app.landpage.output
                copy {
                    from(C.root.app.landpage.originOutput)
                    into(outputAppDir)
                }
                delete(*outputAppDir.asFile.listFiles { it.extension == "map" || it.extension == "txt" })
                zip(outputAppDir, C.root.outputs.file("landpage.zip"))
                delete(outputAppDir)
            }
        }
    }
})