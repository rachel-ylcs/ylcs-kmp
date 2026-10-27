plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.nativeBridge)
}

val explicit = providers.gradleProperty("native.explicit").getOrElse("false").toBoolean()
layout.buildDirectory.set(layout.projectDirectory.dir("../../vendor/build/maven-consumer"))

kotlin {
    compilerOptions { freeCompilerArgs.addAll("-Xexpect-actual-classes", "-Xcollection-literals") }
    mingwX64("windows")
    linuxX64("linux")
    macosArm64("macos")
    sourceSets {
        commonMain.dependencies {
            // Deliberately request only the extension: Core must arrive through its API metadata.
            if (explicit) implementation(libs.native.skiko.skottie)
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
