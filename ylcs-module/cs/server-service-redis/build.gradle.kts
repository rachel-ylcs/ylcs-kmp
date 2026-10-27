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
                    libs.rethis,
                    projects.ylcsModule.cs.serverEngine,
                )
            }

            windowsMain(nativeMain)
            linuxMain(nativeMain)
            macosMain(nativeMain)
        }
    }
}
