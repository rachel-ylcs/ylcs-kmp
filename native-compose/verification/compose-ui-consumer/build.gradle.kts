import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import groovy.json.JsonSlurper
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import java.time.Duration

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.nativeBridge)
}

val generation = gradle.extensions.extraProperties.get("love.yinlin.nativeCompose.verificationGeneration") as File
tasks.configureEach { inputs.property("rachel.native.generation", generation.name) }
layout.buildDirectory.set(generation.resolve("build/compose-ui/maven-consumer"))
val explicit = providers.gradleProperty("native.explicit").getOrElse("false").toBoolean()

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
            binaries.withType<TestExecutable>().configureEach { linkerOpts("-S") }
        }
    }
    sourceSets {
        commonMain.dependencies {
            if (explicit) {
                implementation(nativeBridge.port(libs.compose.foundation))
                implementation(nativeBridge.port(libs.compose.resources))
                implementation(nativeBridge.port(libs.compose.animation.graphics))
                implementation(nativeBridge.port(libs.androidx.navigation3))
                implementation(nativeBridge.port(libs.skiko.lottie))
                implementation(nativeBridge.port(libs.compose.material3))
                implementation(nativeBridge.port(libs.compose.material3.windowSizeClass))
                implementation(nativeBridge.port(libs.compose.material3.adaptive.navigation.suite))
                implementation(nativeBridge.port(libs.compose.material3.adaptive.navigation3))
            }
            else {
                implementation(libs.compose.foundation)
                implementation(libs.compose.resources)
                implementation(libs.compose.animation.graphics)
                implementation(libs.androidx.navigation3.asProvider())
                implementation(libs.skiko.lottie)
                implementation(libs.compose.material3.asProvider())
                implementation(libs.compose.material3.windowSizeClass)
                implementation(libs.compose.material3.adaptive.navigation.suite)
                implementation(libs.compose.material3.adaptive.navigation3)
            }
            implementation(libs.androidx.navigation3.viewmodel)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        val nativeMain = create("nativeMain") { dependsOn(commonMain.get()) }
        val nativeTest = create("nativeTest") { dependsOn(commonTest.get()) }
        for (target in listOf("windows", "linux", "macos")) {
            getByName("${target}Main").dependsOn(nativeMain)
            getByName("${target}Test").dependsOn(nativeTest)
        }
    }
}

nativeBridge { skikoTests() }

tasks.withType<KotlinNativeTest>().configureEach {
    timeout.set(Duration.ofMinutes(5))
    testLogging {
        events(TestLogEvent.STARTED, TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "love.yinlin.nativecompose.consumer.resources"
}

// Explicit Native coordinates must form a complete Maven graph without upstream port requests.
tasks.register("verifyNativeMavenClosure") {
    doLast {
        val metadata = JsonSlurper().parse(generation.resolve("metadata/compose.json")) as Map<*, *>
        val owned = (metadata["modules"] as Map<*, *>).map { (name, value) ->
            val module = value as Map<*, *>
            (module["upstreamGroup"] ?: "org.jetbrains.compose.${module["family"] ?: "ui"}").toString() to name.toString()
        } + listOf("org.jetbrains.skiko" to "skiko", "org.jetbrains.skiko" to "skiko-skottie")
        kotlin.targets.withType<KotlinNativeTarget>().filter { it.konanTarget == HostManager.host }.forEach { target ->
            val result = configurations.getByName(target.compilations.getByName("main").compileDependencyConfigurationName).incoming.resolutionResult
            result.allDependencies.filterIsInstance<org.gradle.api.artifacts.result.UnresolvedDependencyResult>().forEach { throw it.failure }
            result.allComponents.forEach { component ->
                val selected = component.id as? org.gradle.api.artifacts.component.ModuleComponentIdentifier
                check(selected == null || owned.none { (group, artifact) -> selected.group == group && (selected.module == artifact || selected.module.startsWith("$artifact-")) }) {
                    "Native consumer mixed an upstream owned KLIB into the adapted backend graph: ${component.id}"
                }
            }
            if (explicit) {
                result.allDependencies.forEach { dependency ->
                    val requested = dependency.requested as? org.gradle.api.artifacts.component.ModuleComponentSelector
                    check(requested == null || owned.none { (group, artifact) -> requested.group == group && requested.module == artifact }) {
                        "Native Maven publication still needs a redirect: ${dependency.requested}"
                    }
                }
            }
        }
    }
}
