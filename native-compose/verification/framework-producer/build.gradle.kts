import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.nativeBridge)
    `maven-publish`
}

group = "love.yinlin.verification"
version = "1.0"
layout.buildDirectory.set(layout.projectDirectory.dir("../../vendor/build/framework-producer"))

kotlin {
    jvmToolchain(25)
    jvm("desktop")
    // Every CI host verifies its own native publication alongside the JVM variant.
    when (HostManager.host.name) {
        "mingw_x64" -> mingwX64("windows")
        "linux_x64" -> linuxX64("linux")
        "macos_arm64" -> macosArm64("macos")
    }
    sourceSets.commonMain.dependencies {
        api(libs.compose.ui)
        implementation(libs.compose.resources)
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "love.yinlin.verification.resources"
}

publishing.repositories.maven {
    name = "Verification"
    url = uri(providers.gradleProperty("framework.repository").getOrElse("../../vendor/build/framework-repository"))
}

val host = when (HostManager.host.name) {
    "mingw_x64" -> "Windows"
    "linux_x64" -> "Linux"
    "macos_arm64" -> "Macos"
    else -> error("Unsupported verification host")
}
tasks.register("publishHost") {
    dependsOn("publishKotlinMultiplatformPublicationToVerificationRepository", "publishDesktopPublicationToVerificationRepository", "publish${host}PublicationToVerificationRepository")
}
