import love.yinlin.gradle.ExportLib

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
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
                    projects.ylcsModule.compose.core,
                )
            }

            commonTest {
                lib(
                    libs.test,
                    libs.kotlinx.coroutines.test
                )
            }
        }
    }
}
