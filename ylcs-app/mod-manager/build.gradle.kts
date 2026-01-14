import love.yinlin.task.spec.zip
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
                projects.ylcsApp.mod,
                projects.ylcsModule.compose.app,
                projects.ylcsModule.compose.screen,
                projects.ylcsModule.compose.ui.rachel,
                projects.ylcsModule.compose.ui.urlImage,
            )
        }
    }

    override val desktopPackageName: String = C.modManager.name
    override val desktopMainClass: String = C.modManager.mainClass
    override val desktopJvmArgs: List<String> = buildList {
        if ("modManagerRun" in currentTaskName) {
            val desktopWorkSpace = C.root.work.modManager.asFile
            desktopWorkSpace.mkdirs()
            add("-Duser.dir=$desktopWorkSpace")
        }
    }
    override val desktopModules: List<String> = C.desktop.modules.toList()
    override val windowsDistributions: (WindowsPlatformSettings.() -> Unit) = {}

    override fun Project.actions() {
        // 运行 桌面程序 Debug
        val modManagerRunDebug by tasks.registering {
            dependsOn(tasks.named("run"))
        }

        val modManagerPublish by tasks.registering {
            dependsOn(tasks.named("createReleaseDistributable"))

            doLast {
                delete(C.root.outputs.dir(desktopPackageName))
                copy {
                    from(C.root.modManager.originOutput)
                    into(C.root.outputs)
                }
                zip(C.root.outputs.dir(desktopPackageName), C.root.outputs.file("$desktopPackageName.zip"))
                delete(C.root.outputs.dir(desktopPackageName))
            }
        }
    }
})