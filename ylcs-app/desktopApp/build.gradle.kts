import love.yinlin.task.CopyDesktopNativeTask
import love.yinlin.task.spec.zip
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
            dependsOn(findProject(projects.ylcsModule.buildTool)!!.tasks.named("buildDesktopNativePAG"))
        }

        if ("desktopPublish" in currentTaskName) {
            tasks.named("prepareAppResources") {
                dependsOn(desktopCopyNativeLib)
            }
        }

        // 发布桌面应用程序
        val desktopPublish by tasks.registering {
            dependsOn(tasks.named("createReleaseDistributable"))

            doLast {
                delete(packageResourcesDir)
                copy {
                    from(C.root.desktopApp.originOutput)
                    into(C.root.outputs)
                }
                val platformName = when (C.platform) {
                    BuildPlatform.Windows -> "[Windows]"
                    BuildPlatform.Linux -> "[Linux]"
                    BuildPlatform.Mac -> "[MacOS]"
                }
                zip (C.root.outputs.dir(C.app.name).dir("app"), C.root.outputs.file("$platformName${C.app.displayName}${C.app.versionName}升级包.zip"))
                zip (C.root.outputs.dir(C.app.name), C.root.outputs.file("$platformName${C.app.displayName}${C.app.versionName}.zip"))
                delete(C.root.outputs.dir(C.app.name))
            }
        }
    }
})