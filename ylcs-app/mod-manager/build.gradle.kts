import love.yinlin.gradle.desktopBinaryDir
import love.yinlin.gradle.task.spec.zip
import love.yinlin.repository.C
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
                    projects.ylcsModule.compose.components.image,
                )
            }
        }

        val targetName = "ModManager"

        desktopPackage {
            packageName = targetName
            jvmModules = listOf(
                "java.instrument",
                "java.net.http",
                "java.management",
                "jdk.unsupported"
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
                    val outputAppDir = C.root.outputs.dir(targetName)
                    delete(outputAppDir)
                    copy {
                        from(desktopBinaryDir)
                        into(C.root.outputs)
                    }
                    zip(outputAppDir, C.root.outputs.file("$targetName.zip"))
                    delete(outputAppDir)
                }
            }
        }
    }
}
