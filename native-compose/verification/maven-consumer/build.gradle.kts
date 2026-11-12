import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.nativeBridge)
}

val explicit = providers.gradleProperty("native.explicit").getOrElse("false").toBoolean()
val generation = gradle.extensions.extraProperties.get("love.yinlin.nativeCompose.verificationGeneration") as File
tasks.configureEach { inputs.property("rachel.native.generation", generation.name) }
layout.buildDirectory.set(generation.resolve("build/maven-consumer"))

val kotlinLanguage = KotlinVersion.fromVersion(libs.versions.kotlin.get().split('.').take(2).joinToString("."))
val compilerProfile = gradle.extensions.extraProperties

kotlin {
    compilerOptions {
        languageVersion.set(kotlinLanguage)
        apiVersion.set(kotlinLanguage)
        @Suppress("UNCHECKED_CAST")
        freeCompilerArgs.addAll(compilerProfile["rachel.kotlin.compilerArgs"] as List<String>)
    }
    mingwX64("windows")
    linuxX64("linux")
    macosArm64("macos")
    targets.withType<KotlinNativeTarget>().configureEach {
        if (konanTarget.family == Family.OSX && providers.environmentVariable("GITHUB_ACTIONS").orNull == "true") {
            binaries.withType<Executable>().configureEach { linkerOpts("-S") }
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines)
            // Deliberately request only the extension: Core must arrive through its API metadata.
            if (explicit) implementation(nativeBridge.port(libs.skiko.lottie))
            else implementation(libs.skiko.lottie)
            if (providers.gradleProperty("native.legacyRequest").getOrElse("false").toBoolean()) {
                implementation("org.jetbrains.skiko:skiko:0.150.1")
            }
        }
        val nativeMain = create("nativeMain") { dependsOn(commonMain.get()) }
        getByName("windowsMain").dependsOn(nativeMain)
        getByName("linuxMain").dependsOn(nativeMain)
        getByName("macosMain").dependsOn(nativeMain)
    }
}

nativeApplication {
    enabled.set(true)
    skikoRuntime()
    baseName.set("maven-skiko-consumer")
    entryPoint.set("love.yinlin.nativecompose.consumer.main")
    if (providers.gradleProperty("native.window").getOrElse("false").toBoolean()) arguments.add("--window")
}
