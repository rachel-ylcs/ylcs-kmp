import groovy.json.JsonSlurper
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager
import tasks.configuration.configureNativeTarget

plugins {
    kotlin("multiplatform")
    `maven-publish`
    signing
    id("org.gradle.crypto.checksum")
}

val adaptationDirectory = buildFile.parentFile

extensions.extraProperties["rachel.skiko.configure"] = { context: SkikoProjectContext ->
    project.group = "org.jetbrains.skiko"
    extensions.configure<KotlinMultiplatformExtension> {
        if (supportNativeWindows) context.configureNativeTarget(OS.Windows, Arch.X64, mingwX64())
    }
}

apply(from = layout.projectDirectory.file("rachel-upstream.gradle.kts"))

// Configure source roots after upstream has registered every platform task.
extensions.configure<KotlinMultiplatformExtension> {
    sourceSets.configureEach {
        // Cocoa forward declarations belong to the macOS platform fragment.
        // macosArm64 is the only supported macOS target; use its leaf as the source root.
        if (name == "macosMain") kotlin.setSrcDirs(emptyList<String>())
        else {
            val directory = adaptationDirectory.resolve("src/${if (name == "macosArm64Main") "macosMain" else name}/kotlin")
            if (name == "macosArm64Main") kotlin.srcDir(project.file("src/macosMain/kotlin"))
            if (directory.isDirectory) kotlin.srcDir(directory)
        }
    }
}

tasks.withType<CompileSkikoCppTask>().configureEach {
    val platformSourceSet = when {
        name.contains("Windows") -> "windowsMain"
        name.contains("Linux") -> "linuxMain"
        else -> "macosMain"
    }
    for (sourceSet in listOf("nativeMain", platformSourceSet)) {
        val directory = adaptationDirectory.resolve("src/$sourceSet/cpp")
        if (directory.isDirectory) {
            sourceRoots.add(layout.dir(provider { directory }))
            includeHeadersNonRecursive(directory)
        }
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    if (name == "sourcesJar" || name.endsWith("SourcesJar")) {
        from(adaptationDirectory.resolve("src")) {
            include("**/*.cc", "**/*.cpp", "**/*.h", "**/*.hh")
            into("native-sources")
        }
    }
}

// SDL belongs to the Native producer. Consumers receive its bindings, static
// archive and portable system dependencies through this module's publication.
val nativeComposeDirectory = adaptationDirectory.parentFile.parentFile
apply(from = nativeComposeDirectory.parentFile.resolve("build-logic/native-bridge/gradle/skiko-library-tests.gradle.kts"))
val sdlLock = JsonSlurper().parse(nativeComposeDirectory.resolve("upstream/sdl.json")) as Map<*, *>
val sdlVersion = sdlLock["version"] as String
val sdlHost = HostManager.host.name
val sdlSdk = nativeComposeDirectory.resolve("vendor/sdl-static/$sdlHost")
val sdlArchive = if (sdlHost == "mingw_x64") "SDL3-static.lib" else "libSDL3.a"
apply(from = nativeComposeDirectory.parentFile.resolve("build-logic/native-bridge/gradle/sdl-sdk.gradle.kts"))
val prepareSdl = tasks.named("prepareSdlStatic")

if (sdlHost == "mingw_x64") {
    tasks.named<StripCoffDirectivesTask>("stripCoffDirectivesWindowsX64") {
        dependsOn(prepareSdl)
        inputLibraries.add(sdlSdk.resolve("lib/$sdlArchive"))
    }
}

kotlin.targets.withType<KotlinNativeTarget>().configureEach {
    // Published Native libraries retain the identity referenced by official precompiled KLIBs.
    compilations.getByName("main").compileTaskProvider.configure {
        compilerOptions.moduleName.set("org.jetbrains.skiko:skiko")
    }
    val isHost = konanTarget == HostManager.host
    if (isHost) {
        val targetPublication = name
        publishing.publications.withType<MavenPublication>().configureEach {
            if (name == targetPublication) {
                artifact(nativeComposeDirectory.resolve("vendor/icu/workspace/icudtl.dat")) {
                    classifier = "icudtl"
                    extension = "dat"
                }
                artifact(nativeComposeDirectory.resolve("vendor/icu/workspace/icu-LICENSE.txt")) {
                    classifier = "icu-license"
                    extension = "txt"
                }
            }
        }
    }
    val sdlLibraryDirectory = if (sdlHost == "mingw_x64") layout.buildDirectory.dir("nativeBridges/strippedCoff/windows-x64").get().asFile else sdlSdk.resolve("lib")
    val output = layout.buildDirectory.file("cinterop/sdl-$name.def")
    val definition = adaptationDirectory.resolve("src/nativeMain/cinterop/sdl.def")
    val writeDefinition = tasks.register("writeSdlDef${name.replaceFirstChar(Char::uppercase)}") {
        dependsOn(prepareSdl)
        inputs.file(definition)
        if (isHost) {
            inputs.file(sdlSdk.resolve("rachel-sdl.json"))
            if (sdlHost == "mingw_x64") dependsOn("stripCoffDirectivesWindowsX64")
        }
        outputs.file(output)
        doLast {
            val header = if (isHost) {
                val profile = JsonSlurper().parse(sdlSdk.resolve("rachel-sdl.json")) as Map<*, *>
                val options = (profile["linkerOpts"] as List<*>).joinToString(" ")
                "staticLibraries = $sdlArchive\nlinkerOpts = $options\n"
            }
            else ""
            val file = output.get().asFile
            file.parentFile.mkdirs()
            file.writeText(definition.readText().replace("\r\n", "\n").replace("---\n", header + "---\n"))
        }
    }
    val interop = compilations.getByName("main").cinterops.create("sdl") {
        definitionFile.set(output)
        includeDirs(nativeComposeDirectory.resolve("vendor/sdl-source-$sdlVersion/include"))
        if (isHost) extraOpts("-libraryPath", sdlLibraryDirectory.invariantSeparatorsPath)
    }
    tasks.named(interop.interopProcessingTaskName).configure { dependsOn(writeDefinition) }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    if (name == "sourcesJar" || name.endsWith("SourcesJar")) {
        dependsOn(prepareSdl)
        from(adaptationDirectory.resolve("src/nativeMain/cinterop")) { into("nativeInterop/cinterop") }
        from(nativeComposeDirectory.resolve("vendor/sdl-source-$sdlVersion/LICENSE.txt")) {
            into("META-INF/licenses")
            rename { "SDL.txt" }
        }
        from(nativeComposeDirectory.resolve("vendor/icu/workspace/icu-LICENSE.txt")) {
            into("META-INF/licenses")
            rename { "ICU.txt" }
        }
        from(nativeComposeDirectory.resolve("upstream/icu.json")) { into("META-INF/provenance") }
    }
}
