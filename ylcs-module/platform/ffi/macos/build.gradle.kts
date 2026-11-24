import love.yinlin.gradle.ExportLib

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinJvm,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

rachel {
    jvm {
        sources {
            main {
                lib(
                    ExportLib,
                    projects.ylcsModule.platform.ffi.core,
                )
            }

        }
    }
}
