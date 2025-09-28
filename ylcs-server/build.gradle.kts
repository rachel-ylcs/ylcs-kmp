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
                // project
                projects.ylcsCs,
                // kotlinx
                libs.kotlinx.json,
                // ktor
                libs.ktor.json,
                libs.ktor.server,
                libs.ktor.server.host,
                libs.ktor.server.config,
                libs.ktor.server.netty,
                libs.ktor.server.negotiation,
                libs.ktor.server.websockets,
                // middleware
                libs.logback,
                libs.mysql,
                libs.mysql.pool,
                libs.redis,
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