plugins {
    install(
        libs.plugins.gradleApplication,
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
                projects.ylcsModule.serverEngine,
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
        tasks.apply {
            withType<Jar> {
                excludes += C.excludes
            }

            distTar { enabled = false }
            distZip { enabled = false }
            shadowDistTar { enabled = false }
            shadowDistZip { enabled = false }
            jar { enabled = false }
            shadowJar {
                destinationDirectory = C.root.server.outputs
            }
        }

        val cleanFatJar by tasks.registering {
            doLast {
                delete(C.root.server.outputFile)
            }
        }

        val buildFatJar = tasks.named("buildFatJar")
        buildFatJar.get().mustRunAfter(cleanFatJar)

        // 运行服务端
        val run by tasks.named("run")

        val serverRun by tasks.registering {
            dependsOn(run)
        }

        // 发布服务端
        val serverPublish by tasks.registering {
            dependsOn(cleanFatJar)
            dependsOn(buildFatJar)
        }
    }
})