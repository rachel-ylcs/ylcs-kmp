plugins {
    install(
        libs.plugins.kotlinJvm,
        libs.plugins.kotlinSerialization,
        libs.plugins.ktor
    )
}

ktor {
    fatJar {
        archiveFileName = C.server.outputName
    }
}

template(object : KotlinJvmTemplate() {
    override fun KotlinJvmSourceSetsScope.source() {
        main.configure {
            lib(
                projects.ylcsApp.cs,
                projects.ylcsModule.foundation.cs.serverEngine,
            )
        }
    }

    override val jvmName: String = C.app.name
    override val jvmMainClass: String = C.app.mainClass
    override val jvmArgs: List<String> = buildList {
        if ("serverPublish" !in currentTaskName) {
            val desktopWorkSpace = C.root.work.server.asFile
            desktopWorkSpace.mkdirs()
            add("-Duser.dir=$desktopWorkSpace")
        }
    }

    override fun Project.actions() {
        // 运行服务端
        val serverRun by tasks.registering {
            dependsOn(tasks.named("run"))
        }

        // 发布服务端
        val serverPublish by tasks.registering {
            dependsOn(tasks.named("buildFatJar"))

            doLast {
                delete(C.root.outputs.file(C.server.outputName))
                copy {
                    from(C.root.server.originOutput)
                    into(C.root.outputs)
                }
            }
        }
    }
})