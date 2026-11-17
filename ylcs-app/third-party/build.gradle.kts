import love.yinlin.gradle.ExportLib

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.nativeBridge,
        libs.plugins.androidLibrary,
    )
}

rachel {
    multiplatform {
        nativeDesktopTarget = true

        sources {
            commonMain {
                lib(
                    projects.ylcsModule.foundation.cryptography,
                    projects.ylcsModule.foundation.parser.lrc,
                    projects.ylcsModule.foundation.network,

                    libs.compose.runtime,

                    ExportLib,
                    projects.ylcsApp.mod,
                )
            }

            desktopTest {
                lib(
                    libs.test,
                    libs.kotlinx.coroutines.test,
                )
            }
        }
    }
}
