import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

plugins {
    install(libs.plugins.kotlinMultiplatform, libs.plugins.nativeBridge)
}

kotlin {
    compilerOptions { freeCompilerArgs.add("-Xcollection-literals") }
    mingwX64("windows")
    linuxX64("linux")
    macosArm64("macos")
    sourceSets {
        commonMain.dependencies {
            api(libs.skiko)
            implementation(libs.skiko.lottie)
        }
        val nativeMain = create("nativeMain") { dependsOn(commonMain.get()) }
        getByName("windowsMain").dependsOn(nativeMain)
        getByName("linuxMain").dependsOn(nativeMain)
        getByName("macosMain").dependsOn(nativeMain)
        commonTest.dependencies { implementation(kotlin("test")) }
        val nativeTest = create("nativeTest") { dependsOn(commonTest.get()) }
        getByName("windowsTest").dependsOn(nativeTest)
        getByName("linuxTest").dependsOn(nativeTest)
        getByName("macosTest").dependsOn(nativeTest)
        all { languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi") }
    }
}

nativeApplication {
    skikoWindow()
    baseName.set("skiko")
    entryPoint.set("love.yinlin.nativecompose.window.main")
}
kotlin.targets.withType<KotlinNativeTarget>().configureEach {
    val target = this
    val targetName = name
    val stageRuntime = tasks.register<Copy>("stage${targetName.replaceFirstChar(Char::uppercase)}TestRuntime") {
        val testBinary = target.binaries.getTest("DEBUG")
        dependsOn("stage${targetName.replaceFirstChar(Char::uppercase)}DebugExecutable", testBinary.linkTaskProvider)
        from(layout.buildDirectory.dir("nativeApplication/$targetName/debugExecutable")) { include("icudtl.dat") }
        into(project.provider { testBinary.outputFile.parentFile })
    }
    tasks.withType<KotlinNativeTest>().matching { it.name == "${targetName}Test" }.configureEach {
        dependsOn(stageRuntime)
    }
}
