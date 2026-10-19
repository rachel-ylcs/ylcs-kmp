import love.yinlin.gradle.*

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
        linuxTarget = true
        macosTarget = true

        sources {
            nativeMain(commonMain) {
                lib(
                    libs.jetbrains.annotation,
                    libs.sqlx4k,
                    projects.ylcsModule.cs.serverEngine,
                )
            }

            windowsMain(nativeMain)
            linuxMain(nativeMain)
            macosMain(nativeMain)
        }
    }
}
