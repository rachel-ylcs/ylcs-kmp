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
    extensions.configure<KotlinMultiplatformExtension> {
        if (supportNativeWindows) context.configureNativeTarget(OS.Windows, Arch.X64, mingwX64())
    }
}

apply(from = layout.projectDirectory.file("rachel-upstream.gradle.kts"))

// Configure source roots after upstream has registered every platform task.
extensions.configure<KotlinMultiplatformExtension> {
    sourceSets.configureEach {
        val directory = adaptationDirectory.resolve("src/$name/kotlin")
        if (directory.isDirectory) kotlin.srcDir(directory)
    }
}

tasks.withType<CompileSkikoCppTask>().configureEach {
    val platformSourceSet = when {
        name.contains("Windows") -> "windowsMain"
        name.contains("Linux") -> "linuxMain"
        else -> "macosMain"
    }
    for (sourceSet in listOf("commonMain", "nativeMain", platformSourceSet)) {
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
apply(from = nativeComposeDirectory.resolve("scripts/templates/skiko-library-tests.gradle.kts"))
val sdlLock = JsonSlurper().parse(nativeComposeDirectory.resolve("upstream/sdl.json")) as Map<*, *>
val sdlVersion = sdlLock["version"] as String
val sdlHost = HostManager.host.name
val sdlSdk = nativeComposeDirectory.resolve("vendor/sdl-static/$sdlHost")
val sdlArchive = if (sdlHost == "mingw_x64") "SDL3-static.lib" else "libSDL3.a"
val prepareSdl = tasks.register<Exec>("prepareSdlStatic") {
    group = "native setup"
    inputs.files(nativeComposeDirectory.resolve("scripts/prepare_sdl.py"), nativeComposeDirectory.resolve("upstream/sdl.json"))
    outputs.files(sdlSdk.resolve("rachel-sdl.json"), sdlSdk.resolve("lib/$sdlArchive"))
    commandLine(providers.gradleProperty("python.command").getOrElse("python"), nativeComposeDirectory.resolve("scripts/prepare_sdl.py"), "--build")
    if (sdlHost == "linux_x64") {
        dependsOn("downloadKotlinNativeDistribution")
        val nativeLocation = layout.buildDirectory.file("konan.txt")
        inputs.file(nativeLocation)
        args("--konan-home-file", nativeLocation.get().asFile)
    }
}

if (sdlHost == "mingw_x64") {
    tasks.named<StripCoffDirectivesTask>("stripCoffDirectivesWindowsX64") {
        dependsOn(prepareSdl)
        inputLibraries.add(sdlSdk.resolve("lib/$sdlArchive"))
    }
}

kotlin.targets.withType<KotlinNativeTarget>().configureEach {
    val isHost = konanTarget == HostManager.host
    val sdlLibraryDirectory = if (sdlHost == "mingw_x64") layout.buildDirectory.dir("nativeBridges/strippedCoff/windows-x64").get().asFile else sdlSdk.resolve("lib")
    val output = layout.buildDirectory.file("cinterop/sdl-$name.def")
    val definition = adaptationDirectory.resolve("src/nativeInterop/cinterop/sdl.def")
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
        from(adaptationDirectory.resolve("src/nativeInterop")) { into("nativeInterop") }
        from(nativeComposeDirectory.resolve("vendor/sdl-source-$sdlVersion/LICENSE.txt")) {
            into("META-INF/licenses")
            rename { "SDL.txt" }
        }
    }
}
