import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.gradleApplication)
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.ktor.server)
    implementation(libs.ktor.server.host)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.config)
    implementation(libs.ktor.server.negotiation)

    implementation(libs.logback)
    implementation(libs.mysql)
    implementation(libs.mysql.pool)
    implementation(libs.redis)
}

application {
    mainClass.set(rootProject.extra["appMainClass"] as String)
    applicationName = rootProject.extra["appName"] as String
}

tasks.apply {
    distTar { enabled = false }
    distZip { enabled = false }
    shadowDistTar { enabled = false }
    shadowDistZip { enabled = false }
    jar { enabled = false }
    shadowJar {
        archiveFileName = rootProject.extra["serverOutputFileName"] as String
        destinationDirectory = rootProject.extra["serverOutputDir"] as Directory
    }
}