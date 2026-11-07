import love.yinlin.gradle.BuildPlatform
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
        nativeDesktopTarget = true
        iosTarget = false
        webTarget = false
        desktopTarget = false

        sources {
            nativeMain(commonMain) {
                lib(
                    libs.compose.resources,
                    projects.ylcsApp.app.portal,
                    projects.ylcsModule.foundation.os,
                )
            }

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(nativeMain)
        }

        composeDesktopPackage {
            entryPoint = "main"
            packageName = C.app.name
            displayName = C.app.displayName
            iconFile = C.root.app.config.icon.asFile
        }

        artifacts {
            val (target, platform) = when (C.platform) {
                BuildPlatform.Windows -> "windows" to "WindowsNative"
                BuildPlatform.Linux -> "linux" to "LinuxNative"
                BuildPlatform.Mac -> "macos" to "macOSNative"
            }
            val archive = nativeDesktopReleaseArchive
            copy("nativeDesktopArtifact", archive, C.root.outputs, "ylcs-native-$target.zip", "打包当前平台的原生桌面端工件")
            copy("nativeDesktopPublish", archive, C.root.outputs, "[$platform]${C.app.displayName}${C.versionName}.zip", "发布原生桌面端")
        }

        runs {
            nativeDesktop {
                debug("nativeDesktopRunDebug", "运行Debug原生桌面端")
                release("nativeDesktopRunRelease", "运行Release原生桌面端")
                workingDirectory = C.root.work.desktop.asFile
            }
        }
    }
}
