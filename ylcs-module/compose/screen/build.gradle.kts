import love.yinlin.gradle.*

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
                    libs.androidx.navigation3,
                    libs.androidx.navigation3.viewmodel,
                    ExportLib,
                    libs.kotlinx.json,
                    projects.ylcsModule.compose.ui,
                )
            }

            commonTest {
                lib(libs.test)
            }
        }
    }
}
