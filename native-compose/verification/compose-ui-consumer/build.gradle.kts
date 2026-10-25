plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
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
                implementation(libs.native.compose.ui.asProvider())
            }
            else {
                implementation(libs.compose.ui.asProvider())
            }
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

// Explicit Native coordinates must form a complete Maven graph without upstream port requests.
tasks.register("verifyNativeMavenClosure") {
    doLast {
        val artifacts = setOf("ui-util", "ui-geometry", "ui-unit", "ui-graphics", "ui-text", "ui-skiko", "ui-backhandler", "ui")
        kotlin.targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().forEach { target ->
            val result = configurations.getByName(target.compilations.getByName("main").compileDependencyConfigurationName).incoming.resolutionResult
            result.allDependencies.filterIsInstance<org.gradle.api.artifacts.result.UnresolvedDependencyResult>().forEach { throw it.failure }
            result.allComponents.forEach { component ->
                val selected = component.id as? org.gradle.api.artifacts.component.ModuleComponentIdentifier
                check(selected == null || selected.group != "org.jetbrains.compose.ui" || artifacts.none { selected.module == it || selected.module.startsWith("$it-") }) {
                    "Native consumer mixed an upstream UI KLIB into the adapted backend graph: ${component.id}"
                }
            }
            if (explicit) {
                result.allDependencies.forEach { dependency ->
                    val requested = dependency.requested as? org.gradle.api.artifacts.component.ModuleComponentSelector
                    check(requested == null || requested.group != "org.jetbrains.skiko" && !(requested.group == "org.jetbrains.compose.ui" && requested.module in artifacts)) {
                        "Native Maven publication still needs a redirect: ${dependency.requested}"
                    }
                }
            }
        }
    }
}
