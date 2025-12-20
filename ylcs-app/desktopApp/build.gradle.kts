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
            lib(libs.compose.components.resources)
        }

        desktopMain.configure(commonMain) {
            lib(
                projects.ylcsApp.shared,
                projects.ylcsModule.autoUpdate,
                projects.ylcsModule.singleInstance,
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
            add("-Djava.library.path=${C.root.native.libs}")
        }
    }
    override val desktopModules: List<String> = C.desktop.modules.toList()
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
        val run = tasks.named("run")
        val runRelease = tasks.named("runRelease")
        val createReleaseDistributable = tasks.named("createReleaseDistributable")
        val suggestRuntimeModules = tasks.named("suggestRuntimeModules")

        // 运行 桌面程序 Debug
        val desktopRunDebug by tasks.registering {
            dependsOn(run)
        }

        // 运行 桌面程序 Release
        val desktopRunRelease by tasks.registering {
            dependsOn(runRelease)
        }

        // 检查桌面模块完整性
        val desktopCheckModules by tasks.registering {
            dependsOn(suggestRuntimeModules)
        }

        val desktopCopyDir by tasks.registering {
            mustRunAfter(createReleaseDistributable)
            doLast {
                copy {
                    from(C.root.desktopApp.originOutput)
                    into(C.root.outputs)
                }
                copy {
                    from(C.root.native.libs)
                    into(C.root.desktopApp.libOutput)
                }
                copy {
                    from(C.root.config.currentPackages)
                    into(C.root.desktopApp.packagesOutput)
                }
                val platformName = when (C.platform) {
                    BuildPlatform.Windows -> "[Windows]"
                    BuildPlatform.Linux -> "[Linux]"
                    BuildPlatform.Mac -> "[MacOS]"
                }
                zip {
                    from(C.root.outputs.dir(C.app.name).dir("app"))
                    to(C.root.outputs.file("$platformName${C.app.displayName}${C.app.versionName}升级包.zip"))
                }
                zip {
                    from(C.root.outputs.dir(C.app.name))
                    to(C.root.outputs.file("$platformName${C.app.displayName}${C.app.versionName}.zip"))
                }
                delete {
                    delete(C.root.outputs.dir(C.app.name))
                }
            }
        }

        // 发布桌面应用程序
        val desktopPublish by tasks.registering {
            dependsOn(createReleaseDistributable)
            dependsOn(desktopCopyDir)
        }
    }
})