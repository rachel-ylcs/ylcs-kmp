import love.yinlin.gradle.ExportLib
import love.yinlin.gradle.BuildPlatform
import love.yinlin.gradle.task.BuildDesktopNativeTask
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
            desktop {
                cpp("src/nativeMain/cpp")
            }
            native {
                cpp("audio")
            }
        }

        sources {
            commonMain {
                lib(
                    libs.compose.resources,
                    ExportLib,
                    projects.ylcsModule.foundation.filesystem,
                    projects.ylcsModule.compose.ui,
                    projects.ylcsModule.compose.platformView,
                )
            }

            androidMain(commonMain) {
                lib(
                    ExportLib,
                    libs.media3.ui,
                    libs.media3.player,
                    libs.media3.session,
                )
            }

            iosMain(commonMain)

            iosMainList(iosMain)

            pcMain(commonMain)

            desktopMain(pcMain) {
                lib(projects.ylcsModule.foundation.libloader)
            }

            nativeMain(pcMain)

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(nativeMain)

            webMain(commonMain) {
                lib(projects.ylcsModule.compose.platformView)
            }

            jsMain(webMain)

            wasmJsMain(webMain)
        }
    }
}

val winrtSdk = tasks.register<PrepareNuGetPackageTask>("prepareWinRtSdk") {
    packageId.set("Microsoft.Windows.SDK.CPP")
    packageVersion.set(libs.versions.windowsSdkCpp)
}
val winrtArchive = winrtSdk.flatMap { it.archive }
tasks.withType<BuildDesktopNativeTask>().configureEach {
    if (nativePlatform.get() == BuildPlatform.Windows) {
        dependsOn(winrtSdk)
        inputs.file(winrtArchive)
        cmakeOptions.add(winrtArchive.map { "RACHEL_WINRT_SDK_ARCHIVE" to it.asFile.invariantSeparatorsPath })
    }
}
tasks.withType<BuildNativeCppTask>().matching { it.name == "buildWindowsNative" }.configureEach {
    dependsOn(winrtSdk)
    inputs.file(winrtArchive)
    cmakeOptions.add(winrtArchive.map { "RACHEL_WINRT_SDK_ARCHIVE" to it.asFile.invariantSeparatorsPath })
}
