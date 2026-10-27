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
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

rachel {
    multiplatform {
        nativeDesktopTarget = true

        sources {
            commonMain {
                lib(
                    libs.androidx.navigation3.runtime,
                    libs.androidx.navigation3.viewmodel,
                    libs.androidx.navigation3,
                    ExportLib,
                    projects.ylcsModule.compose.ui,
                )
            }

            commonTest {
                lib(libs.test)
            }
        }
    }
}
