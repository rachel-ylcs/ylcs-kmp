import love.yinlin.gradle.*

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

            test {
                lib(
                    libs.test,
                    libs.kotlinx.coroutines.test,
                )
            }
        }
    }
}
