import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.gradleApplication)
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    implementation(projects.shared)

    implementation(libs.kotlinx.json)

    implementation(libs.ktor.json)
    implementation(libs.ktor.server)
    implementation(libs.ktor.server.host)
    implementation(libs.ktor.server.config)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.negotiation)
    implementation(libs.ktor.server.cors)

    implementation(libs.logback)
    implementation(libs.mysql)
    implementation(libs.mysql.pool)
    implementation(libs.redis)
}

application {
    mainClass.set(rootProject.extra["appMainClass"] as String)
    applicationName = rootProject.extra["appName"] as String

    val taskName = project.gradle.startParameter.taskNames.firstOrNull() ?: ""
    if (!taskName.contains("serverPublish")) {
        val serverCurrentDir: Directory by rootProject.extra
        serverCurrentDir.asFile.mkdir()
    }
}

val outputFileName = rootProject.extra["serverOutputFileName"] as String
val outputDir = rootProject.extra["serverOutputDir"] as Directory
val outputFile = outputDir.file(outputFileName)

ktor {
    fatJar {
        archiveFileName = outputFileName
    }
}

tasks.withType<Jar> {
    excludes += arrayOf(
        "/META-INF/{AL2.0,LGPL2.1}",
        "DebugProbesKt.bin"
    )
}

afterEvaluate {
    tasks.apply {
        distTar { enabled = false }
        distZip { enabled = false }
        shadowDistTar { enabled = false }
        shadowDistZip { enabled = false }
        jar { enabled = false }
        shadowJar {
            destinationDirectory = outputDir
        }
    }

    val cleanFatJar by tasks.registering {
        doLast {
            delete(outputFile)
        }
    }

    val buildFatJar = tasks.named("buildFatJar")
    buildFatJar.get().mustRunAfter(cleanFatJar)

    // 发布服务端
    val serverPublish: Task by tasks.creating {
        dependsOn(cleanFatJar)
        dependsOn(buildFatJar)
    }
}