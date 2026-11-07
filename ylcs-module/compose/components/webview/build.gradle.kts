import love.yinlin.gradle.ExportLib
import love.yinlin.gradle.task.BuildNativeCppTask
import love.yinlin.gradle.task.PrepareNuGetPackageTask

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.nativeBridge,
        libs.plugins.androidLibrary,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

rachel {
    multiplatform {
        nativeDesktopTarget = true

        interop {
            native {
                windows {
                    cpp("webview")
                }
                macos {
                    cpp("webviewObservation")
                }
            }
        }

        sources {
            commonMain {
                lib(
                    projects.ylcsModule.compose.ui,
                    ExportLib,
                    projects.ylcsModule.compose.platformView,
                )
            }

            iosMain(commonMain)

            iosMainList(iosMain)

            nativeMain(commonMain)

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(nativeMain)

            webMain(commonMain)

            jsMain(webMain)

            wasmJsMain(webMain)
        }

        actions {
            val sdk = tasks.register<PrepareNuGetPackageTask>("prepareWebView2Sdk") {
                packageId.set("Microsoft.Web.WebView2")
                packageVersion.set(libs.versions.webview2)
            }
            val archive = sdk.flatMap { it.archive }
            tasks.withType<BuildNativeCppTask>().matching { it.name == "buildWindowsNative" }.configureEach {
                dependsOn(sdk)
                inputs.file(archive)
                cmakeOptions.add(archive.map { "RACHEL_WEBVIEW2_ARCHIVE" to it.asFile.invariantSeparatorsPath })
            }
        }
    }
}
