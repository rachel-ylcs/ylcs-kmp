import love.yinlin.gradle.ExportLib

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

rachel {
    nativeLibrary {
        windowsTarget = true

        sources {
            windowsMain {
                lib(
                    ExportLib,
                    projects.ylcsModule.core,
                )
            }
        }
    }
}
