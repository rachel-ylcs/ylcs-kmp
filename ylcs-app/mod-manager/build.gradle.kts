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
            workingDirectory = C.root.work.modManager.asFile
        }

        actions {
            tasks.withType<Zip>().matching { it.name.startsWith("package") && it.name.endsWith("ReleaseExecutable") }.configureEach {
                archiveVersion.set("")
            }

            val (target, platform) = when (C.platform) {
                BuildPlatform.Windows -> "Windows" to "WindowsNative"
                BuildPlatform.Linux -> "Linux" to "LinuxNative"
                BuildPlatform.Mac -> "Macos" to "macOSNative"
            }
            val archive = tasks.named<Zip>("package${target}ReleaseExecutable")
            for ((taskName, filename) in mapOf(
                "modManagerArtifact" to "ylcs-mod-manager-${target.lowercase()}.zip",
                "modManagerPublish" to "[$platform]ModManager${C.versionName}.zip",
            )) {
                tasks.register<Copy>(taskName) {
                    description = if (taskName == "modManagerArtifact") "打包MOD编辑器工件" else "发布MOD编辑器"
                    dependsOn(archive)

                    from(archive.flatMap { it.archiveFile })
                    into(C.root.outputs)
                    rename { filename }
                }
            }

            tasks.register("modManagerRunDebug") {
                description = "运行MOD编辑器Debug原生桌面版"
                dependsOn(tasks.named("composeRun"))
            }
        }
    }
}
