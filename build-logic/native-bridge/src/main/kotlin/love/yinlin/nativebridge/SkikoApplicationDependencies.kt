package love.yinlin.nativebridge

import groovy.json.JsonSlurper
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import java.util.Properties

/** Runtime/SDK conventions used by staged samples and Rachel applications alike. */
internal object SkikoApplicationDependencies {
    fun configure(project: Project, kotlin: KotlinMultiplatformExtension, application: NativeApplicationExtension,
                  bridge: NativeBridgeExtension, compatibility: Properties) {
        val extras = project.gradle.extensions.extraProperties
        val nativeRoot = if (extras.has("love.yinlin.nativeCompose.root")) extras.get("love.yinlin.nativeCompose.root") as File else null
        if (application.skikoRuntimeEnabled.get()) configureSkiko(project, application, bridge, nativeRoot)
        if (!application.skikoWindowEnabled.get()) return
        val version = compatibility.getProperty("sdl.version")
        val override = project.providers.gradleProperty("sdl.root").orNull?.let(project.rootProject::file)
        require(nativeRoot != null || override != null) { "nativeApplication.skikoWindow requires -Psdl.root=<SDL3 prefix> outside source-maintainer mode" }
        val prepare = if (nativeRoot != null && override == null) prepareSdl(project, nativeRoot, version) else null
        kotlin.targets.withType(KotlinNativeTarget::class.java).configureEach {
            val target = this
            val windows = konanTarget.name == "mingw_x64"
            val macos = konanTarget.name == "macos_arm64"
            val sdk = override ?: nativeRoot!!.resolve(when {
                windows -> "vendor/sdl-windows-$version/x86_64-w64-mingw32"
                macos -> "vendor/sdl-macos"
                else -> "vendor/sdl-linux"
            })
            if (application.sdlDefinition.isPresent) {
                val interop = compilations.getByName("main").cinterops.create("sdl") {
                    definitionFile.set(project.file(application.sdlDefinition.get()))
                    includeDirs(if (override != null) sdk.resolve("include") else nativeRoot!!.resolve("vendor/sdl-source-$version/include"))
                }
                if (prepare != null) project.tasks.named(interop.interopProcessingTaskName).configure { dependsOn(prepare) }
            }
            binaries.configureEach {
                linkerOpts("-L${sdk.resolve("lib").absolutePath}", "-lSDL3")
                if (macos) linkerOpts("-Wl,-rpath,@loader_path")
                else if (!windows) linkerOpts("-Wl,-rpath,\$ORIGIN")
                if (prepare != null) linkTaskProvider.configure { dependsOn(prepare) }
            }
            application.runtime(konanTarget.name) {
                files.from(if (windows) project.files(sdk.resolve("bin/SDL3.dll")) else project.fileTree(sdk.resolve("lib")) {
                    include(if (macos) "libSDL3*.dylib" else "libSDL3.so*")
                })
                if (prepare != null) files.builtBy(prepare)
            }
        }
        if (project.providers.gradleProperty("native.smoke").getOrElse("false").toBoolean()) application.arguments.add("--smoke")
        val renderer = project.providers.gradleProperty("native.renderer").getOrElse("gpu")
        require(renderer in setOf("gpu", "software")) { "native.renderer must be gpu or software" }
        if (renderer == "software") application.arguments.add("--software")
    }

    private fun configureSkiko(project: Project, application: NativeApplicationExtension, bridge: NativeBridgeExtension, nativeRoot: File?) {
        if (nativeRoot != null) {
            val state = JsonSlurper().parse(nativeRoot.resolve("vendor/skiko/state.json")) as Map<*, *>
            val lock = JsonSlurper().parse(nativeRoot.resolve("upstream/lock.json")) as Map<*, *>
            val skia = lock["skia"] as String
            val build = nativeRoot.resolve(state["build"] as String)
            application.resources.from(nativeRoot.resolve("licenses"))
            application.runtime("mingw_x64") {
                files.from(build.resolve("dependencies/skia/$skia/Skia-$skia-windows-Release-x64/out/Release-windows-x64/icudtl.dat"))
                files.builtBy(project.gradle.includedBuild("skiko").task(":unzipSkiaReleaseWindowsX64"))
            }
        }
        else {
            val module = bridge.redirects.getByName("skiko-core").module.get().split(':')
            application.runtime("mingw_x64") {
                artifacts.put("${module[0]}:${module[1]}-mingwx64:${module[2]}:icudtl@dat", "icudtl.dat")
            }
        }
    }

    private fun prepareSdl(project: Project, nativeRoot: File, version: String): TaskProvider<Exec> {
        val tasks = project.rootProject.tasks
        val name = "prepareNativeComposeSdl"
        if (name in tasks.names) return tasks.named(name, Exec::class.java)
        val host = HostManager.host.name
        val sdk = nativeRoot.resolve(when (host) {
            "mingw_x64" -> "vendor/sdl-windows-$version/x86_64-w64-mingw32"
            "macos_arm64" -> "vendor/sdl-macos"
            else -> "vendor/sdl-linux"
        })
        return tasks.register(name, Exec::class.java) {
            group = "native setup"
            inputs.file(nativeRoot.resolve("upstream/sdl.json"))
            outputs.dir(sdk)
            commandLine(project.providers.gradleProperty("python.command").getOrElse("python"), nativeRoot.resolve("scripts/prepare_sdl.py"), "--build")
            onlyIf { !sdk.resolve(".rachel-sdl-version").isFile || sdk.resolve(".rachel-sdl-version").readText().trim() != version }
        }
    }
}
