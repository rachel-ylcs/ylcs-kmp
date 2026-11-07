import love.yinlin.gradle.BuildPlatform
import love.yinlin.repository.C

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
    )
}

rachel {
    nativeExecutable {
        nativeDesktopTarget = true

        sources {
            nativeMain(commonMain) {
                lib(
                    projects.ylcsApp.cs,

                    projects.ylcsModule.foundation.cryptography,

                    projects.ylcsModule.cs.serverEngine,
                    projects.ylcsModule.cs.serverServiceMysql,
                    projects.ylcsModule.cs.serverServiceRedis,
                )
            }

            windowsMain(nativeMain)
            linuxMain(nativeMain)
            macosMain(nativeMain)
        }

        artifacts {
            val binary = releaseExecutable(BuildPlatform.Linux)
            copy("serverArtifact", binary, C.root.outputs, description = "打包服务端工件")
            copy("serverPublish", binary, C.root.outputs, description = "发布服务端")
        }

        runs {
            nativeExecutable {
                arguments = listOf("--cd=${C.root.work.server.asFile}")
            }
        }
    }
}
