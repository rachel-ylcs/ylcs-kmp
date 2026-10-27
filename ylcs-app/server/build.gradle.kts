import love.yinlin.gradle.nativeBinaryDir
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

        commandArgs = listOf("--cd=${C.root.work.server.asFile}")

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

        actions {
            tasks.register("serverPublish") {
                description = "发布服务端"
                dependsOn(tasks.named("linkReleaseExecutableLinux"))

                doLast {
                    delete(C.root.outputs.file("server.kexe"))
                    copy {
                        from(nativeBinaryDir("linux", "server.kexe"))
                        into(C.root.outputs)
                    }
                }
            }
        }
    }
}
