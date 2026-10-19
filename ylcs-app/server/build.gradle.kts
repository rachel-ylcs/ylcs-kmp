import love.yinlin.gradle.*
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
        windowsTarget = true
        linuxTarget = true
        macosTarget = true

        args = listOf("--cd=${C.root.work.server.asFile}")

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
                    delete(C.root.outputs.file("${uniqueName}.kexe"))
                    copy {
                        from(C.root.app.server.originOutput)
                        into(C.root.outputs)
                    }
                }
            }
        }
    }
}
