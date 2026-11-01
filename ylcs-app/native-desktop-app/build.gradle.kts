import love.yinlin.gradle.BuildPlatform
import love.yinlin.repository.C
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip

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
            workingDirectory = C.root.work.desktop.asFile
        }

        actions {
            tasks.register("nativeDesktopRunDebug") {
                description = "运行Debug原生桌面端"
                dependsOn(tasks.named("composeRun"))
            }

            tasks.register("nativeDesktopRunRelease") {
                description = "运行Release原生桌面端"
                dependsOn(tasks.named("composeReleaseRun"))
            }

            val (target, platform) = when (C.platform) {
                BuildPlatform.Windows -> "Windows" to "WindowsNative"
                BuildPlatform.Linux -> "Linux" to "LinuxNative"
                BuildPlatform.Mac -> "Macos" to "macOSNative"
            }
            val archive = tasks.named<Zip>("package${target}ReleaseExecutable")

            for ((taskName, filename) in mapOf(
                "nativeDesktopArtifact" to "ylcs-native-${target.lowercase()}.zip",
                "nativeDesktopPublish" to "[$platform]${C.app.displayName}${C.versionName}.zip",
            )) {
                tasks.register<Copy>(taskName) {
                    description = if (taskName == "nativeDesktopArtifact") "打包当前平台的原生桌面端工件" else "发布原生桌面端"
                    dependsOn(archive)

                    from(archive.flatMap { it.archiveFile })
                    into(C.root.outputs)
                    rename { filename }
                }
            }
        }
    }
}
