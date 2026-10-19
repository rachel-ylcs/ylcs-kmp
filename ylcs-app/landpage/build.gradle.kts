import love.yinlin.gradle.*
import love.yinlin.repository.C
import love.yinlin.gradle.task.spec.zip
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
                port = C.host.webServerPort
                client?.overlay = false
                open = false
            }
        }

        actions {
            tasks.register("landpageRun") {
                description = "运行落地页"
                dependsOn(tasks.named("jsBrowserDevelopmentRun"))
            }

            tasks.register("landpagePublish") {
                description = "发布落地页"
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
    }
}
