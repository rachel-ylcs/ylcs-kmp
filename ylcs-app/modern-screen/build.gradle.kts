import love.yinlin.gradle.wasmBinaryDir
import love.yinlin.repository.C
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.nativeBridge,
    )
}

rachel {
    multiplatform {
        iosTarget = false
        nativeDesktopTarget = true

        sources {
            commonMain {
                lib(
                    projects.ylcsModule.compose.app,
                    projects.ylcsModule.compose.screen,
                )
            }

            desktopMain(commonMain)

            nativeMain(commonMain)

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(nativeMain)

            wasmJsMain(commonMain)
        }

        desktopPackage {
            windows { }
        }

        composeDesktopPackage {
            entryPoint = "main"
            packageName = "RachelModernScreen"
            displayName = "Rachel Modern Screen"
        }

        webpack {
            outputFileName = "$uniqueSafeModuleName.js"

            devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                port = C.web.webServerPort
                client?.overlay = false
            }
        }

        actions {
            tasks.register("modernScreenRun") {
                description = "运行Debug原生桌面版ModernScreen"
                dependsOn(tasks.named("composeRun"))
            }

            tasks.register("modernScreenPublish") {
                description = "发布网页版ModernScreen"
                dependsOn(tasks.named("wasmJsBrowserDistribution"))

                doLast {
                    copy {
                        from(wasmBinaryDir)
                        into(C.root.docs.modernScreen)
                    }
                }
            }
        }
    }
}
