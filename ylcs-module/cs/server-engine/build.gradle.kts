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
        nativeDesktopTarget = true

        sources {
            nativeMain(commonMain) {
                lib(
                    libs.ktor.json,
                    libs.ktor.server.cio,
                    libs.ktor.server.negotiation,
                    libs.ktor.server.websockets,
                    ExportLib,
                    libs.ktor.server,
                    projects.ylcsModule.foundation.filesystem,
                    projects.ylcsModule.cs.core,
                )
            }

            windowsMain(nativeMain)
            linuxMain(nativeMain)
            macosMain(nativeMain)
        }
    }
}
