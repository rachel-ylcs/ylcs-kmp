import love.yinlin.gradle.*
import love.yinlin.repository.C
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

        desktopPackage {
            packageName = C.modManager.name
            mainClass = C.modManager.mainClass
            jvmModules = C.desktop.modules.toList()
            proguard = listOf(
                projects.ylcsModule.core,
                projects.ylcsModule.cs.core,
                projects.ylcsModule.foundation.network,
                projects.ylcsModule.compose.core,
                projects.ylcsModule.compose.components.urlImage,
            )
            windows { }
        }

        actions {
            val desktopWorkspace = C.root.work.modManager.asFile
            tasks.withType<JavaExec>().matching { it.name == "run" || it.name == "runRelease" }.configureEach {
                jvmArgs("-Duser.dir=$desktopWorkspace")
                doFirst { desktopWorkspace.mkdirs() }
            }

            tasks.register("modManagerRunDebug") {
                description = "运行MOD编辑器Debug桌面版"
                dependsOn(tasks.named("run"))
            }

            tasks.register("modManagerPublish") {
                description = "发布MOD编辑器桌面版"
                dependsOn(tasks.named("createReleaseDistributable"))

                doLast {
                    val outputAppDir = C.root.outputs.dir(C.modManager.name)
                    delete(outputAppDir)
                    copy {
                        from(C.root.app.modManager.originOutput)
                        into(C.root.outputs)
                    }
                    zip(outputAppDir, C.root.outputs.file("${C.modManager.name}.zip"))
                    delete(outputAppDir)
                }
            }
        }
    }
}
