plugins {
    alias(libs.plugins.gradleApplication)
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    C.useCompilerFeatures(this)
    C.jvmTarget(this)

    sourceSets {
        main.configure {
            useLib(
                projects.ylcsApp.cs,
                projects.ylcsModule.serverEngine,
            )
        }
    }
}

application {
    mainClass.set(C.app.mainClass)
    applicationName = C.app.name

    applicationDefaultJvmArgs = buildList {
        if ("serverPublish" !in currentTaskName) {
            val desktopWorkSpace = C.root.work.server.asFile
            desktopWorkSpace.mkdirs()
            add("-Duser.dir=$desktopWorkSpace")
        }
        add("--enable-native-access=ALL-UNNAMED")
    }
}

ktor {
    fatJar {
        archiveFileName = C.server.outputName
    }
}

tasks.withType<Jar> {
    excludes += C.excludes
}

afterEvaluate {
    tasks.apply {
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