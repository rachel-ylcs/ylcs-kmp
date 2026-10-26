import groovy.json.JsonSlurper
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.nativeBridge)
}

layout.buildDirectory.set(layout.projectDirectory.dir("../../vendor/build/compose-ui/maven-consumer"))
val explicit = providers.gradleProperty("native.explicit").getOrElse("false").toBoolean()

kotlin {
    compilerOptions.freeCompilerArgs.addAll("-Xexpect-actual-classes", "-Xcollection-literals")
    mingwX64("windows")
    linuxX64("linux")
    macosArm64("macos")
    sourceSets {
        commonMain.dependencies {
            if (explicit) {
                implementation(libs.native.compose.foundation.asProvider())
                implementation(libs.native.compose.resources)
                implementation(libs.native.compose.animation.graphics)
                implementation(libs.native.androidx.navigation3)
                implementation(libs.native.skiko.skottie)
            }
            else {
                implementation(libs.compose.foundation)
                implementation(libs.compose.resources)
                implementation("org.jetbrains.compose.animation:animation-graphics:${libs.versions.cmp.get()}")
                implementation(libs.androidx.navigation3.asProvider())
                implementation(libs.skiko.lottie)
            }
            implementation(libs.androidx.navigation3.viewmodel)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        val nativeMain = create("nativeMain") { dependsOn(commonMain.get()) }
        val nativeTest = create("nativeTest") { dependsOn(commonTest.get()) }
        for (target in listOf("windows", "linux", "macos")) {
            getByName("${target}Main").dependsOn(nativeMain)
            getByName("${target}Test").dependsOn(nativeTest)
        }
    }
}

nativeBridge { skikoTests() }

compose.resources {
    publicResClass = true
    packageOfResClass = "love.yinlin.nativecompose.consumer.resources"
}

// Explicit Native coordinates must form a complete Maven graph without upstream port requests.
tasks.register("verifyNativeMavenClosure") {
    doLast {
        val lock = JsonSlurper().parse(file("../../upstream/compose.json")) as Map<*, *>
        val owned = (lock["modules"] as Map<*, *>).map { (name, value) ->
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
