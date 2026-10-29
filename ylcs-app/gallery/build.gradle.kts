import love.yinlin.gradle.wasmBinaryDir
import love.yinlin.repository.C
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

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
        nativeDesktopTarget = true

        sources {
            commonMain {
                lib(
                    libs.compose.resources,
                    projects.ylcsModule.foundation.fts,
                    projects.ylcsModule.compose.app,
                    projects.ylcsModule.compose.components.image,
                    projects.ylcsModule.compose.components.blur,
                    projects.ylcsModule.compose.components.calendar,
                    projects.ylcsModule.compose.components.dataTable,
                    projects.ylcsModule.compose.components.dragDrop,
                    projects.ylcsModule.compose.components.hexview,
                    projects.ylcsModule.compose.components.logOutput,
                    projects.ylcsModule.compose.components.reordering,
                    projects.ylcsModule.compose.components.richText,
                )
            }

            desktopMain(commonMain)

            nativeMain(commonMain)

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(nativeMain)

            wasmJsMain(commonMain)
        }

        desktopPackage {
            windows { }
        }

        composeDesktopPackage {
            entryPoint = "main"
            packageName = "RachelGallery"
            displayName = "Rachel UI Gallery"
        }

        webpack {
            outputFileName = "$uniqueSafeModuleName.js"

            devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                port = C.web.webServerPort
                client?.overlay = false
            }
        }

        actions {
            tasks.register("galleryRun") {
                description = "运行Debug原生桌面版Gallery"
                dependsOn(tasks.named("composeRun"))
            }

            tasks.register("galleryPublish") {
                description = "发布网页版Gallery"
                dependsOn(tasks.named("wasmJsBrowserDistribution"))

                doLast {
                    copy {
                        from(wasmBinaryDir)
                        into(C.root.docs.gallery)
                    }
                }
            }
        }
    }
}
