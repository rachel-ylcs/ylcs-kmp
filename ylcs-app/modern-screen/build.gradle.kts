import love.yinlin.gradle.BrowserArtifactTarget
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

        artifacts {
            sync("modernScreenPublish", browserDistribution(BrowserArtifactTarget.Wasm), C.root.docs.modernScreen, "发布网页版ModernScreen")
        }

        runs {
            jvm {
                debug("modernScreenRun", "运行Debug桌面版ModernScreen")
            }

            nativeDesktop {
                debug("nativeModernScreenRun", "运行Debug原生桌面版ModernScreen")
            }
        }
    }
}
