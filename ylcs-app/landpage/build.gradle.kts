import love.yinlin.gradle.*
import love.yinlin.repository.C
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
    )
}

rachel {
    js {
        sources {
            jsMain(commonMain) {
                lib(
                    libs.kotlinx.html,
                    projects.ylcsModule.core,
                    projects.ylcsApp.cs,
                )
            }
        }

        webpack {
            devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                port = C.web.webServerPort
                client?.overlay = false
                open = false
            }
        }

        actions {
            tasks.register("landpageRun") {
                description = "运行落地页"
                dependsOn(tasks.named("jsBrowserDevelopmentRun"))
            }

            tasks.register<Zip>("landpagePublish") {
                description = "发布落地页"
                dependsOn(tasks.named("jsBrowserDistribution"))

                from(jsBinaryDir) {
                    into("landpage")
                    exclude("*.map", "*.txt")
                }
                destinationDirectory.set(C.root.outputs)
                archiveFileName.set("landpage.zip")
                doFirst { delete(C.root.outputs.dir("landpage")) }
            }
        }
    }
}
