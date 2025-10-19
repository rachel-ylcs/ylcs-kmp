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

    if ("serverPublish" !in currentTaskName) C.root.server.workspace.asFile.mkdir()
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

    // 发布服务端
    val serverPublish by tasks.registering {
        dependsOn(cleanFatJar)
        dependsOn(buildFatJar)
    }
}