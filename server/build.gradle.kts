import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val projectBuildDir: Directory get() = project.layout.buildDirectory.get()
val outputName: String = "ylcs.jar"
val outputDir: Directory get() = projectBuildDir.dir("production")

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
    implementation(libs.server.ktor.core)
    implementation(libs.server.ktor.host)
    implementation(libs.server.ktor.netty)
    implementation(libs.server.ktor.config)
    implementation(libs.server.ktor.negotiation)

    implementation(libs.server.logback)
    implementation(libs.server.mysql)
    implementation(libs.server.mysql.pool)
    implementation(libs.server.redis)
}

application {
    mainClass.set("love.yinlin.ApplicationKt")
    applicationName = "ylcs-server"
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=false")
}

tasks.apply {
    distTar { enabled = false }
    distZip { enabled = false }
    shadowDistTar { enabled = false }
    shadowDistZip { enabled = false }
    jar { enabled = false }
    shadowJar {
        archiveFileName = outputName
        destinationDirectory = outputDir
    }
}