plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
    )
}

template(object : KotlinNativeExecutableTemplate() {
    override val windowsTarget: Boolean = true
    override val linuxTarget: Boolean = true
    override val macosTarget: Boolean = true

    override val args: List<String> = buildList {
        if ("serverPublish" !in currentTaskName) add("--cd=${C.root.work.server.asFile}")
    }

    override fun KotlinNativeSourceSetsScope.source() {
        nativeMain.configure(commonMain) {
            lib(
                libs.cryptography,
                libs.cryptography.provider,
                projects.ylcsApp.cs,
                projects.ylcsModule.cs.serverEngine,
                projects.ylcsModule.cs.serverServiceMysql,
                projects.ylcsModule.cs.serverServiceRedis,
            )
        }

        windowsMain.configure(nativeMain)
        linuxMain.configure(nativeMain)
        macosMain.configure(nativeMain)
    }

    override fun Project.actions() {
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
})