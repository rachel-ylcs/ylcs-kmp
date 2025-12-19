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
    override val androidTarget: Boolean = false
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
                projects.ylcsModule.compose.component.urlImage,
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
        val run = tasks.named("run")
        val createReleaseDistributable = tasks.named("createReleaseDistributable")

        // 运行 桌面程序 Debug
        val modManagerRunDebug by tasks.registering {
            dependsOn(run)
        }

        val modManagerCopyDir by tasks.registering {
            mustRunAfter(createReleaseDistributable)
            doLast {
                copy {
                    from(C.root.modManager.originOutput)
                    into(C.root.outputs)
                }
            }
        }

        val modManagerPublish by tasks.registering {
            dependsOn(createReleaseDistributable)
            dependsOn(modManagerCopyDir)
        }
    }
})