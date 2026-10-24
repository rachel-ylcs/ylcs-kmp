import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    install(libs.plugins.kotlinMultiplatform, libs.plugins.nativeBridge)
}

val nativeDirectory = project(":native-compose").layout.projectDirectory
val sdlVersion = libs.versions.sdl.get()
val windowsSdl = nativeDirectory.dir("vendor/sdl-windows-$sdlVersion/x86_64-w64-mingw32").asFile
val posixSdl = providers.gradleProperty("sdl.root").map { rootProject.file(it) }

kotlin {
    compilerOptions { freeCompilerArgs.add("-Xcollection-literals") }
    mingwX64("windows")
    linuxX64("linux")
    macosArm64("macos")
    sourceSets {
        commonMain.dependencies { implementation(libs.skiko) }
        val desktopNativeMain = create("desktopNativeMain") { dependsOn(commonMain.get()) }
        getByName("windowsMain").dependsOn(desktopNativeMain)
        getByName("linuxMain").dependsOn(desktopNativeMain)
        getByName("macosMain").dependsOn(desktopNativeMain)
    }
    targets.withType<KotlinNativeTarget>().configureEach {
        val isWindows = konanTarget.name == "mingw_x64"
        val isMacos = konanTarget.name == "macos_arm64"
        val sdk = if (isWindows) windowsSdl else posixSdl.orNull ?:
            nativeDirectory.dir(if (isMacos) "vendor/sdl-macos" else "vendor/sdl-linux").asFile
        compilations.getByName("main") {
            cinterops.create("sdl") {
                definitionFile.set(project.file("src/nativeInterop/cinterop/sdl.def"))
                // Public SDL headers are portable; cinterop still uses each Konan target's ABI.
                // An installed prefix wins, while source headers also allow off-host IDE import.
                includeDirs(if (isWindows || sdk.resolve("include").isDirectory) sdk.resolve("include")
                    else nativeDirectory.dir("vendor/sdl-source-$sdlVersion/include").asFile)
            }
        }
        binaries.configureEach {
            linkerOpts("-L${sdk.resolve("lib").absolutePath}", "-lSDL3")
            if (isMacos) linkerOpts("-Wl,-rpath,@loader_path")
            else if (!isWindows) linkerOpts("-Wl,-rpath,\$ORIGIN")
        }
    }
}

nativeApplication {
    enabled.set(true)
    baseName.set("skiko-window")
    entryPoint.set("love.yinlin.nativecompose.window.main")
    if (providers.gradleProperty("native.smoke").getOrElse("false").toBoolean()) arguments.add("--smoke")
    runtime("mingw_x64") {
        files.from(windowsSdl.resolve("bin/SDL3.dll"))
    }
    runtime("linux_x64") {
        // Include the complete SONAME chain from the explicitly selected prefix.
        files.from(fileTree(posixSdl.orNull?.resolve("lib") ?: nativeDirectory.dir("vendor/sdl-linux/lib").asFile) {
            include("libSDL3.so*")
        })
    }
    runtime("macos_arm64") {
        files.from(fileTree(posixSdl.orNull?.resolve("lib") ?: nativeDirectory.dir("vendor/sdl-macos/lib").asFile) {
            include("libSDL3*.dylib")
        })
    }
}
