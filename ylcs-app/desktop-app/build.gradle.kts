import love.yinlin.gradle.BuildPlatform
import love.yinlin.gradle.desktopBinaryDir
import love.yinlin.repository.C
import love.yinlin.gradle.task.CopyDesktopNativeTask
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Zip

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

        actions {
            val desktopWorkspace = C.root.work.desktop.asFile
            val desktopNativeDirectory = C.root.artifacts.desktopNative.asFile
            tasks.withType<JavaExec>().matching { it.name == "run" || it.name == "runRelease" }.configureEach {
                jvmArgs(
                    "-Duser.dir=$desktopWorkspace",
                    "-Dnative.library.resource.disabled=true",
                    "-Djava.library.path=$desktopNativeDirectory",
                )
                doFirst { desktopWorkspace.mkdirs() }
            }

            tasks.register("desktopRunDebug") {
                description = "运行Debug桌面端"
                dependsOn(tasks.named("run"))
            }

            tasks.register("desktopRunRelease") {
                description = "运行Release桌面端"
                dependsOn(tasks.named("runRelease"))
            }

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

            val (platform, applicationDirectory) = when (C.platform) {
                BuildPlatform.Windows -> "Windows" to targetName
                BuildPlatform.Linux -> "Linux" to targetName
                BuildPlatform.Mac -> "macOS" to "$targetName.app"
            }
            val executablePath = when (C.platform) {
                BuildPlatform.Windows -> "$applicationDirectory/$targetName.exe"
                BuildPlatform.Linux -> "$applicationDirectory/bin/$targetName"
                BuildPlatform.Mac -> "$applicationDirectory/Contents/MacOS/$targetName"
            }
            val executable = desktopBinaryDir.file(executablePath)
            for ((taskName, filename) in mapOf(
                "desktopArtifact" to "ylcs-${platform.lowercase()}.zip",
                "desktopPublish" to "[$platform]${C.app.displayName}${C.versionName}.zip",
            )) {
                tasks.register<Zip>(taskName) {
                    description = if (taskName == "desktopArtifact") "打包当前平台的桌面端工件" else "发布桌面端"
                    dependsOn(tasks.named("createReleaseDistributable"))

                    from(desktopBinaryDir) { include("$applicationDirectory/**") }
                    destinationDirectory.set(C.root.outputs)
                    archiveFileName.set(filename)
                    useFileSystemPermissions()
                    inputs.file(executable).withPropertyName("desktopExecutable")
                    doFirst { delete(C.root.outputs.dir(applicationDirectory)) }
                }
            }
        }
    }
}
