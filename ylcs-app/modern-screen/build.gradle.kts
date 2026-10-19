import love.yinlin.gradle.*
import love.yinlin.repository.C
import org.jetbrains.compose.desktop.application.dsl.WindowsPlatformSettings
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
    )
}

rachel {
    multiplatform {
        iosTarget = false

        sources {
            commonMain {
                lib(projects.ylcsModule.compose.screen)
            }

            desktopMain(commonMain)

            wasmJsMain(commonMain)
        }

        desktopPackage {
            packageName = uniqueSafeName
            mainClass = C.app.mainClass
            windows { }
        }

        webpack {
            outputFileName = "$uniqueSafeModuleName.js"

            devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                port = C.host.webServerPort
                client?.overlay = false
            }
        }

        actions {
            tasks.register("modernScreenRun") {
                description = "运行Debug桌面版ModernScreen"
                dependsOn(tasks.named("run"))
            }

            tasks.register("modernScreenPublish") {
                description = "发布网页版ModernScreen"
                dependsOn(tasks.named("wasmJsBrowserDistribution"))

                doLast {
                    copy {
                        from(C.root.app.modernScreen.originOutput)
                        into(C.root.app.modernScreen.output)
                    }
                }
            }
        }
    }
}
