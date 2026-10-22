import love.yinlin.gradle.BuildPlatform
import love.yinlin.gradle.desktopBinaryDir
import love.yinlin.gradle.packageResourcesDir
import love.yinlin.repository.C
import love.yinlin.gradle.task.CopyDesktopNativeTask
import love.yinlin.gradle.task.spec.zip
import org.gradle.api.tasks.JavaExec

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
            commonMain {
                lib(libs.compose.resources)
            }

            desktopMain(commonMain) {
                lib(
                    projects.ylcsApp.app.portal,
                    projects.ylcsModule.platform.nativeLibLoader,
                    projects.ylcsModule.platform.os.singleInstance,
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

            tasks.register("desktopArtifact") {
                description = "桌面端打包"
                dependsOn(tasks.named("createReleaseDistributable"))

                doLast {
                    val outputDir = C.root.outputs
                    delete(packageResourcesDir)
                    copy {
                        from(desktopBinaryDir)
                        into(outputDir)
                    }

                    when (C.platform) {
                        BuildPlatform.Windows -> zip(outputDir.dir(targetName), outputDir.file("ylcs-windows.zip"))
                        BuildPlatform.Linux -> zip(outputDir.dir(targetName), outputDir.file("ylcs-linux.zip"))
                        BuildPlatform.Mac -> zip(outputDir.dir("$targetName.app"), outputDir.file("ylcs-macos.zip"))
                    }
                }
            }

            tasks.register("desktopPublish") {
                description = "发布桌面端"
                dependsOn(tasks.named("createReleaseDistributable"))

                doLast {
                    val outputDir = C.root.outputs
                    delete(packageResourcesDir)
                    copy {
                        from(desktopBinaryDir)
                        into(outputDir)
                    }

                    val artifactName = "${C.app.displayName}${C.versionName}"
                    val outputAppDir = outputDir.dir(targetName)
                    when (C.platform) {
                        BuildPlatform.Windows -> zip(outputAppDir, outputDir.file("[Windows]${artifactName}.zip"))
                        BuildPlatform.Linux -> zip(outputAppDir, outputDir.file("[Linux]${artifactName}.zip"))
                        BuildPlatform.Mac -> zip(outputDir.dir("$targetName.app"), outputDir.file("[macOS]${artifactName}.zip"))
                    }

                    delete(outputAppDir)
                }
            }
        }
    }
}
