import love.yinlin.gradle.BrowserArtifactTarget
import love.yinlin.repository.C

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

        webDevProxy(port = C.web.webServerPort, outputFileName = "$uniqueSafeModuleName.js")

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
