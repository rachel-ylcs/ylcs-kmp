import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager

buildscript {
    if (providers.gradleProperty("framework.native").getOrElse("false").toBoolean()) {
        repositories {
            maven { url = uri(providers.gradleProperty("native.plugin.repository").getOrElse("../../../build-logic/build/native-bridge-repository")) }
            mavenCentral()
            gradlePluginPortal()
        }
        // JVM mode does not request the bridge plugin or its implementation.
        val bridgeVersion = Regex("(?m)^rachel\\s*=\\s*\"([^\"]+)\"").find(file("../../../libs.versions.toml").readText())!!.groupValues[1]
        dependencies { classpath("love.yinlin.native:native-bridge:$bridgeVersion") }
    }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

val native = providers.gradleProperty("framework.native").getOrElse("false").toBoolean()
if (native) apply(plugin = "love.yinlin.native-bridge")
layout.buildDirectory.set(layout.projectDirectory.dir("../../vendor/build/framework-consumer-${if (native) "native" else "jvm"}"))

kotlin {
    jvmToolchain(25)
    if (native) {
        when (HostManager.host.name) {
            "mingw_x64" -> mingwX64("windows")
            "linux_x64" -> linuxX64("linux")
            "macos_arm64" -> macosArm64("macos")
        }
    }
    else jvm("desktop")
    targets.withType<KotlinNativeTarget>().configureEach {
        if (konanTarget.family == Family.OSX && providers.environmentVariable("GITHUB_ACTIONS").orNull == "true") {
            binaries.withType<TestExecutable>().configureEach { linkerOpts("-S") }
        }
    }
    sourceSets {
        commonMain.dependencies { implementation("love.yinlin.verification:framework:1.0") }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines)
        }
    }
}

tasks.register("verifyFrameworkResolution") {
    doLast {
        check(pluginManager.hasPlugin("love.yinlin.native-bridge") == native)
        kotlin.targets.filter { if (native) it is KotlinNativeTarget && it.konanTarget == HostManager.host else it.name == "desktop" }.forEach { target ->
            val graph = configurations.getByName(target.compilations.getByName("main").compileDependencyConfigurationName).incoming.resolutionResult
            graph.allDependencies.filterIsInstance<UnresolvedDependencyResult>().forEach { throw it.failure }
            val modules = graph.allComponents.mapNotNull { it.id as? ModuleComponentIdentifier }
            check(modules.any { it.group == "love.yinlin.verification" && it.module.startsWith("framework") })
            check(modules.any { it.group == if (native) "love.yinlin.native.compose.ui" else "org.jetbrains.compose.ui" })
            check(native || modules.none { it.group.startsWith("love.yinlin.native.") })
        }
        println("PASS: ordinary framework Maven coordinates / ${if (native) "Native with bridge" else "JVM without bridge"}")
    }
}
