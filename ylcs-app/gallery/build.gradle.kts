import love.yinlin.gradle.BrowserArtifactTarget
import love.yinlin.repository.C

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
                    libs.jetbrains.annotation,
                    libs.compose.resources,
                    projects.ylcsModule.foundation.fts,
                    projects.ylcsModule.compose.app,
                    projects.ylcsModule.compose.platformView,
                    projects.ylcsModule.compose.shader,
                    projects.ylcsModule.compose.components.image,
                    projects.ylcsModule.compose.components.blur,
                    projects.ylcsModule.compose.components.palette,
                    projects.ylcsModule.compose.components.calendar,
                    projects.ylcsModule.compose.components.dataTable,
                    projects.ylcsModule.compose.components.dragDrop,
                    projects.ylcsModule.compose.components.hexview,
                    projects.ylcsModule.compose.components.logOutput,
                    projects.ylcsModule.compose.components.picker,
                    projects.ylcsModule.compose.components.reordering,
                    projects.ylcsModule.compose.components.richText,
                    projects.ylcsModule.compose.components.paginationLayout,
                )
            }

            desktopMain(commonMain)

            nativeMain(commonMain)

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(nativeMain)

            webMain(commonMain)

            jsMain(webMain)

            wasmJsMain(webMain)
        }

        desktopPackage {
            windows { }
        }

        composeDesktopPackage {
            entryPoint = "main"
            packageName = "RachelGallery"
            displayName = "Rachel UI Gallery"
        }

        webDevProxy(port = C.web.webServerPort, outputFileName = "$uniqueSafeModuleName.js")

        artifacts {
            sync("galleryPublish", browserDistribution(BrowserArtifactTarget.Wasm), C.root.docs.gallery, "发布网页版Gallery")
        }

        runs {
            jvm {
                debug("galleryRun", "运行Debug桌面版Gallery")
            }

            nativeDesktop {
                debug("nativeGalleryRun", "运行Debug原生桌面版Gallery")
            }
        }
    }
}
