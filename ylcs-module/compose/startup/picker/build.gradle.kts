import love.yinlin.gradle.*

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.androidLibrary,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

rachel {
    multiplatform {
        sources {
            commonMain {
                lib(
                    ExportLib,
                    projects.ylcsModule.foundation.filesystem,
                    projects.ylcsModule.foundation.startup,
                )
            }

            iosMain(commonMain)

            iosMainList(iosMain)

            desktopMain(commonMain) {
                lib(projects.ylcsModule.platform.nativeLibLoader)
            }

            webMain(commonMain)

            jsMain(webMain)

            wasmJsMain(webMain)
        }
    }
}
