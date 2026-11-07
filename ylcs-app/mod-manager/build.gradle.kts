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
        iosTarget = false
        desktopTarget = false
        webTarget = false
        nativeDesktopTarget = true

        sources {
            commonMain {
                lib(
                    libs.compose.resources,
                    projects.ylcsModule.foundation.compression,
                    projects.ylcsApp.mod,
                    projects.ylcsModule.compose.app,
                    projects.ylcsModule.compose.screen,
                    projects.ylcsModule.compose.components.dragDrop,
                    projects.ylcsModule.compose.components.image,
                )
            }

            nativeMain(commonMain)

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(nativeMain)
        }

        composeDesktopPackage {
            entryPoint = "main"
            packageName = "ModManager"
            displayName = "Rachel MOD Manager"
            versionedArchive = false
        }

        artifacts {
            val (target, platform) = when (C.platform) {
                BuildPlatform.Windows -> "windows" to "WindowsNative"
                BuildPlatform.Linux -> "linux" to "LinuxNative"
                BuildPlatform.Mac -> "macos" to "macOSNative"
            }
            val archive = nativeDesktopReleaseArchive
            copy("modManagerArtifact", archive, C.root.outputs, "ylcs-mod-manager-$target.zip", "打包MOD编辑器工件")
            copy("modManagerPublish", archive, C.root.outputs, "[$platform]ModManager${C.versionName}.zip", "发布MOD编辑器")
        }

        runs {
            nativeDesktop {
                debug("modManagerRunDebug", "运行MOD编辑器Debug原生桌面版")
                release()
                workingDirectory = C.root.work.modManager.asFile
            }
        }
    }
}
