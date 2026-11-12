import groovy.json.JsonSlurper
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.skiko.build.configuration.configureNativeTarget
import org.jetbrains.skiko.build.context.SkikoProjectContext
import org.jetbrains.skiko.build.context.supportNativeWindows
import org.jetbrains.skiko.build.cpp.CompileSkikoCppTask
import org.jetbrains.skiko.build.cpp.StripCoffDirectivesTask
import org.jetbrains.skiko.build.utils.Arch
import org.jetbrains.skiko.build.utils.OS

plugins {
    kotlin("multiplatform")
    `maven-publish`
    signing
    id("org.gradle.crypto.checksum")
    id("love.yinlin.native-dokka") apply false
}

val adaptationDirectory = buildFile.parentFile.canonicalFile
val nativeComposeDirectory = adaptationDirectory.parentFile.parentFile
val generation = rootDir.resolve("../../..").canonicalFile
val dokkaBuild = providers.gradleProperty("rachel.dokka").orNull == "true"
if (dokkaBuild) layout.buildDirectory.set(generation.resolve("build/dokka/skiko"))
allprojects {
    tasks.configureEach { inputs.property("rachel.native.generation", generation.name) }
}

extensions.extraProperties["rachel.skiko.configure"] = { context: SkikoProjectContext ->
    project.group = "org.jetbrains.skiko"
    extensions.configure<KotlinMultiplatformExtension> {
        if (supportNativeWindows) context.configureNativeTarget(OS.Windows, Arch.X64, mingwX64())
    }
}

apply(from = layout.projectDirectory.file("rachel-upstream.gradle.kts"))

if (dokkaBuild) {
    apply(plugin = "love.yinlin.native-dokka")
    extensions.configure<DokkaExtension> {
        modulePath.set("native-compose/skiko")
        dokkaPublications.named("html") { moduleName.set("Native Compose / Skiko") }
    }
}

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
apply(from = nativeComposeDirectory.parentFile.resolve("build-logic/native-bridge/gradle/skiko-library-tests.gradle.kts"))
val sdlHost = HostManager.host.name
val sdlArchive = if (sdlHost == "mingw_x64") "SDL3-static.lib" else "libSDL3.a"
apply(from = nativeComposeDirectory.parentFile.resolve("build-logic/native-bridge/gradle/sdl-sdk.gradle.kts"))
val prepareSdl = tasks.named("prepareSdlStatic")
val sdlSdk = extensions.extraProperties["rachel.sdl.sdk"] as File

if (sdlHost == "mingw_x64" && !dokkaBuild) {
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
                artifact(generation.resolve("icu/workspace/text/icudtl.dat")) {
                    classifier = "icudtl-text"
                    extension = "dat"
                }
                artifact(generation.resolve("icu/workspace/icudtl.dat")) {
                    classifier = "icudtl"
                    extension = "dat"
                }
                artifact(generation.resolve("icu/workspace/icu-LICENSE.txt")) {
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
        if (!dokkaBuild) dependsOn(prepareSdl)
        inputs.file(definition)
        inputs.property("dokkaBuild", dokkaBuild)
        if (isHost && !dokkaBuild) {
            inputs.file(sdlSdk.resolve("rachel-sdl.json"))
            if (sdlHost == "mingw_x64") dependsOn("stripCoffDirectivesWindowsX64")
        }
        outputs.file(output)
        doLast {
            val header = if (isHost && !dokkaBuild) {
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
        includeDirs(generation.resolve("sdl/source/include"))
        if (isHost && !dokkaBuild) extraOpts("-libraryPath", sdlLibraryDirectory.invariantSeparatorsPath)
    }
    tasks.named(interop.interopProcessingTaskName).configure {
        dependsOn(writeDefinition)
        if (isHost && !dokkaBuild) inputs.file(sdlSdk.resolve("lib/$sdlArchive"))
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    if (name == "sourcesJar" || name.endsWith("SourcesJar")) {
        dependsOn(prepareSdl)
        from(adaptationDirectory.resolve("src/nativeMain/cinterop")) { into("nativeInterop/cinterop") }
        from(generation.resolve("sdl/source/LICENSE.txt")) {
            into("META-INF/licenses")
            rename { "SDL.txt" }
        }
        from(generation.resolve("icu/workspace/icu-LICENSE.txt")) {
            into("META-INF/licenses")
            rename { "ICU.txt" }
        }
        from(generation.resolve("metadata/icu.json")) { into("META-INF/provenance") }
    }
}
