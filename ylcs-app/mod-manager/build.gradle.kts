import love.yinlin.task.spec.zip
import org.gradle.api.internal.catalog.DelegatingProjectDependency
import org.jetbrains.compose.desktop.application.dsl.WindowsPlatformSettings

plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override val iosTarget: Boolean = false
    override val webTarget: Boolean = false

    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                libs.compose.resources,
                projects.ylcsApp.mod,
                projects.ylcsModule.compose.app,
                projects.ylcsModule.compose.screen,
                projects.ylcsModule.compose.components.dragDrop,
                projects.ylcsModule.compose.components.urlImage,
            )
        }
    }

    override val desktopPackage = object : DesktopPackage(), DesktopPackage.Windows {
        override val packageName: String = C.modManager.name
        override val mainClass: String = C.modManager.mainClass
        override val jvmArgs: List<String> = buildList {
            if ("modManagerRun" in currentTaskName) {
                val desktopWorkSpace = C.root.work.modManager.asFile
                desktopWorkSpace.mkdirs()
                add("-Duser.dir=$desktopWorkSpace")
            }
        }
        override val jvmModules: List<String> = C.desktop.modules.toList()
        override val proguard: List<DelegatingProjectDependency> = listOf(
            projects.ylcsModule.core,
            projects.ylcsModule.cs.core,
            projects.ylcsModule.foundation.network,
            projects.ylcsModule.compose.core,
            projects.ylcsModule.compose.components.urlImage,
        )
        override fun onSettings(settings: WindowsPlatformSettings) { }
    }

    override fun Project.actions() {
        tasks.register("modManagerRunDebug") {
            description = "运行MOD编辑器Debug桌面版"
            dependsOn(tasks.named("run"))
        }

        tasks.register("modManagerPublish") {
            description = "发布MOD编辑器桌面版"
            dependsOn(tasks.named("createReleaseDistributable"))

            doLast {
                val outputAppDir = C.root.outputs.dir(desktopPackage.packageName)
                delete(outputAppDir)
                copy {
                    from(C.root.app.modManager.originOutput)
                    into(C.root.outputs)
                }
                zip(outputAppDir, C.root.outputs.file("${desktopPackage.packageName}.zip"))
                delete(outputAppDir)
            }
        }
    }
})