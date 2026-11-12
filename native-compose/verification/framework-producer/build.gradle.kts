import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
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
val generation = gradle.extensions.extraProperties.get("love.yinlin.nativeCompose.verificationGeneration") as File
tasks.configureEach { inputs.property("rachel.native.generation", generation.name) }
layout.buildDirectory.set(generation.resolve("build/framework-producer"))

apply(from = "../../gradle/compiler-warnings.gradle.kts")

val kotlinLanguage = KotlinVersion.fromVersion(libs.versions.kotlin.get().split('.').take(2).joinToString("."))
val compilerProfile = gradle.extensions.extraProperties

kotlin {
    compilerOptions {
        languageVersion.set(kotlinLanguage)
        apiVersion.set(kotlinLanguage)
        @Suppress("UNCHECKED_CAST")
        freeCompilerArgs.addAll(compilerProfile["rachel.kotlin.compilerArgs"] as List<String>)
    }
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
    url = uri(providers.gradleProperty("framework.repository").getOrElse(generation.resolve("build/framework-repository").path))
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
