import love.yinlin.repository.C
import org.gradle.api.tasks.bundling.Zip

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.nativeBridge,
    )
}

rachel {
    multiplatform {
        iosTarget = false
        desktopTarget = false
        webTarget = false
        nativeDesktopTarget = true

        sources {
            commonMain {
                lib(
                    libs.compose.resources,
                    projects.ylcsModule.foundation.compression,
                    projects.ylcsApp.mod,
                    projects.ylcsModule.compose.app,
                    projects.ylcsModule.compose.screen,
                    projects.ylcsModule.compose.components.dragDrop,
                    projects.ylcsModule.compose.components.image,
                )
            }

            nativeMain(commonMain)

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(nativeMain)
        }

        composeDesktopPackage {
            entryPoint = "main"
            packageName = "ModManager"
            displayName = "Rachel MOD Manager"
            workingDirectory = C.root.work.modManager.asFile
        }

        actions {
            tasks.withType<Zip>().matching { it.name.startsWith("package") && it.name.endsWith("ReleaseExecutable") }.configureEach {
                archiveVersion.set("")
            }

            tasks.register("modManagerRunDebug") {
                description = "运行MOD编辑器Debug原生桌面版"
                dependsOn(tasks.named("composeRun"))
            }
        }
    }
}
