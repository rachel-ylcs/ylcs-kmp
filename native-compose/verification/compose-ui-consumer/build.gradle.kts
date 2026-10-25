plugins {
    alias(libs.plugins.kotlinMultiplatform)
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
                implementation(libs.native.compose.ui.util)
                implementation(libs.native.compose.ui.geometry)
                implementation(libs.native.compose.ui.unit)
            }
            else {
                implementation(libs.compose.ui.util)
                implementation(libs.compose.ui.geometry)
                implementation(libs.compose.ui.unit)
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
