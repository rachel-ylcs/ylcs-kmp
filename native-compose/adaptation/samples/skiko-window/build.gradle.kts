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
    }
}

nativeApplication {
    skikoWindow("src/nativeInterop/cinterop/sdl.def")
    baseName.set("skiko-window")
    entryPoint.set("love.yinlin.nativecompose.window.main")
}
