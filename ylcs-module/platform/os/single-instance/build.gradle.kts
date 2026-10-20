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
                    projects.ylcsModule.platform.ffi.posix,
                    projects.ylcsModule.platform.ffi.win32,
                )
            }
        }
    }
}
