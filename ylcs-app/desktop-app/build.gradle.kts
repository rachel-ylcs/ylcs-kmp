import love.yinlin.gradle.BuildPlatform
import love.yinlin.gradle.desktopNativeLibraries
import love.yinlin.repository.C
import love.yinlin.gradle.task.CopyDesktopNativeTask

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
        webTarget = false

        sources {
            desktopMain {
                lib(
                    libs.compose.resources,
                    projects.ylcsApp.app.portal,
                    projects.ylcsModule.foundation.os,
                )
            }
        }

        val targetName = C.app.name

        desktopPackage {
            useAOT = true
            packageName = targetName
            jvmModules = listOf(
                "java.instrument",
                "java.net.http",
                "java.management",
                "jdk.unsupported"
            )
            windows {
                iconFile.set(C.root.app.config.icon)
            }
            linux {
                iconFile.set(C.root.app.config.icon)
            }
            macos {
                iconFile.set(C.root.app.config.icon)
            }
        }

        runs {
            jvm {
                debug("desktopRunDebug", "运行Debug桌面端")
                release("desktopRunRelease", "运行Release桌面端")
                workingDirectory = C.root.work.desktop.asFile
                jvmArgs = listOf("-Dnative.library.resource.disabled=true")
                libraryDirectories = listOf(C.root.artifacts.desktopNative.asFile)
                buildDesktopNativeLibraries = true
            }
        }

        actions {
            val runtimeNativeLibraries = providers.provider { desktopNativeLibraries() }
            tasks.named("desktopJar") { dependsOn(runtimeNativeLibraries) }

            tasks.register("desktopCheckModules") {
                description = "检查桌面端模块完整性"
                dependsOn(tasks.named("suggestRuntimeModules"))
            }

            val desktopCopyNativeLib = tasks.register<CopyDesktopNativeTask>("desktopCopyNativeLib") {
                description = "复制桌面端动态库"
                dependsOn(tasks.named("desktopJar"))
            }

            tasks.named("prepareAppResources") {
                dependsOn(desktopCopyNativeLib)
            }
        }

        artifacts {
            val platform = when (C.platform) {
                BuildPlatform.Windows -> "Windows"
                BuildPlatform.Linux -> "Linux"
                BuildPlatform.Mac -> "macOS"
            }
            val distribution = desktopReleaseDistribution
            val executable = desktopReleaseExecutable
            for ((taskName, filename) in mapOf(
                "desktopArtifact" to "ylcs-${platform.lowercase()}.zip",
                "desktopPublish" to "[$platform]${C.app.displayName}${C.versionName}.zip",
            )) {
                zip(
                    taskName, distribution, C.root.outputs, filename,
                    description = if (taskName == "desktopArtifact") "打包当前平台的桌面端工件" else "发布桌面端",
                    archiveRoot = desktopReleaseDirectoryName,
                    executable = executable,
                    replaceUnpackedOutput = true,
                )
            }
        }
    }
}
