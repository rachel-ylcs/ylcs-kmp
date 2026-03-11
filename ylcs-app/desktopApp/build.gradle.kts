import love.yinlin.task.CopyDesktopNativeTask
import love.yinlin.task.spec.zip
import org.gradle.api.internal.catalog.DelegatingProjectDependency
import org.jetbrains.compose.desktop.application.dsl.JvmMacOSPlatformSettings
import org.jetbrains.compose.desktop.application.dsl.LinuxPlatformSettings
import org.jetbrains.compose.desktop.application.dsl.WindowsPlatformSettings

plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.kotlinSerialization,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override val iosTarget: Boolean = false
    override val webTarget: Boolean = false

    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(libs.compose.resources)
        }

        desktopMain.configure(commonMain) {
            lib(
                projects.ylcsApp.app.portal,
                projects.ylcsModule.platform.nativeLibLoader,
                projects.ylcsModule.platform.os.autoUpdate,
                projects.ylcsModule.platform.os.singleInstance,
            )
        }
    }

    override val desktopPackageName: String = C.app.name
    override val desktopMainClass: String = C.app.mainClass
    override val desktopJvmArgs: List<String> = buildList {
        if ("desktopRun" in currentTaskName) {
            val desktopWorkSpace = C.root.work.desktop.asFile
            desktopWorkSpace.mkdirs()
            add("-Duser.dir=$desktopWorkSpace")
            // 不使用资源方式加载 native 库
            add("-Dnative.library.resource.disabled=true")
            add("-Djava.library.path=${C.root.artifacts.desktopNative}")
        }
    }
    override val desktopModules: List<String> = C.desktop.modules.toList()
    override val desktopProguard: List<DelegatingProjectDependency> = listOf(
        projects.ylcsModule.core,
        projects.ylcsModule.cs.core,
        projects.ylcsModule.cs.clientEngine,
        projects.ylcsModule.platform.ffi.core,
        projects.ylcsModule.platform.os.singleInstance,
        projects.ylcsModule.compose.core,
        projects.ylcsModule.compose.components.urlImage,
        projects.ylcsModule.compose.components.media,
    )
    override val windowsDistributions: (WindowsPlatformSettings.() -> Unit) = {
        iconFile.set(C.root.config.icon)
    }
    override val linuxDistributions: (LinuxPlatformSettings.() -> Unit) = {
        iconFile.set(C.root.config.icon)
    }
    override val macOSDistributions: (JvmMacOSPlatformSettings.() -> Unit) = {
        iconFile.set(C.root.config.icon)
    }

    override fun Project.actions() {
        // 运行 桌面程序 Debug
        val desktopRunDebug by tasks.registering {
            dependsOn(tasks.named("run"))
        }

        // 运行 桌面程序 Release
        val desktopRunRelease by tasks.registering {
            dependsOn(tasks.named("runRelease"))
        }

        // 检查桌面模块完整性
        val desktopCheckModules by tasks.registering {
            dependsOn(tasks.named("suggestRuntimeModules"))
        }

        // 复制桌面动态库
        val desktopCopyNativeLib by tasks.registering(CopyDesktopNativeTask::class) {
            dependsOn(tasks.named("desktopJar"))
        }

        val taskName = currentTaskName
        if ("desktopPublish" in taskName || "desktopArtifact" in taskName) {
            tasks.named("prepareAppResources") {
                dependsOn(desktopCopyNativeLib)
            }
        }

        val desktopArtifact by tasks.registering {
            dependsOn(tasks.named("createReleaseDistributable"))

            doLast {
                val outputDir = C.root.outputs
                delete(packageResourcesDir)
                copy {
                    from(C.root.desktopApp.originOutput)
                    into(outputDir)
                }

                when (C.platform) {
                    BuildPlatform.Windows -> {
                        zip(outputDir.dir(C.app.name).dir("app"), outputDir.file("ylcs-windows-upgrade.zip"))
                        zip(outputDir.dir(C.app.name), outputDir.file("ylcs-windows.zip"))
                    }
                    BuildPlatform.Linux -> {
                        zip(outputDir.dir(C.app.name).dir("lib").dir("app"), outputDir.file("ylcs-linux-upgrade.zip"))
                        zip(outputDir.dir(C.app.name), outputDir.file("ylcs-linux.zip"))
                    }
                    BuildPlatform.Mac -> {
                        zip(outputDir.dir("${C.app.name}.app"), outputDir.file("ylcs-macos.zip"))
                    }
                }
            }
        }

        // 发布桌面应用程序
        val desktopPublish by tasks.registering {
            dependsOn(tasks.named("createReleaseDistributable"))

            doLast {
                val outputDir = C.root.outputs
                delete(packageResourcesDir)
                copy {
                    from(C.root.desktopApp.originOutput)
                    into(outputDir)
                }

                val artifactName = "${C.app.displayName}${C.app.versionName}"
                when (C.platform) {
                    BuildPlatform.Windows -> {
                        zip(outputDir.dir(C.app.name).dir("app"), outputDir.file("[Windows]${artifactName}升级包.zip"))
                        zip(outputDir.dir(C.app.name), outputDir.file("[Windows]${artifactName}.zip"))
                    }
                    BuildPlatform.Linux -> {
                        zip(outputDir.dir(C.app.name).dir("lib").dir("app"), outputDir.file("[Linux]${artifactName}升级包.zip"))
                        zip(outputDir.dir(C.app.name), outputDir.file("[Linux]${artifactName}.zip"))
                    }
                    BuildPlatform.Mac -> {
                        zip(outputDir.dir("${C.app.name}.app"), outputDir.file("[macOS]${artifactName}.zip"))
                    }
                }

                delete(outputDir.dir(C.app.name))
            }
        }
    }
})