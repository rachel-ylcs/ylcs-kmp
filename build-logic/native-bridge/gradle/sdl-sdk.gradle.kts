import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import java.security.MessageDigest

fun sdkDigest(file: File): String = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

fun Project.sdkOutput(arguments: List<String>, variables: Map<String, String>): String {
    val result = providers.exec {
        commandLine(arguments)
        environment(variables)
        isIgnoreExitValue = true
    }
    val output = result.standardOutput.asText.get()
    check(result.result.get().exitValue == 0) { "SDL SDK command failed: ${arguments.first()}\n${result.standardError.asText.get()}\n$output" }
    return output
}

fun Project.sdkWindowsEnvironment(base: Map<String, String>, work: File): Map<String, String> {
    if (!base["INCLUDE"].isNullOrBlank() && !base["LIB"].isNullOrBlank()) return base
    val installation = base["VS_PATH"] ?: base["SKIKO_VSBT_PATH"] ?: error("Static SDL requires VS_PATH or an initialized MSVC environment")
    val vcvars = File(installation, "VC/Auxiliary/Build/vcvars64.bat")
    check(vcvars.isFile) { "Missing MSVC environment script: $vcvars" }
    work.mkdirs()
    val helper = work.resolve("msvc-environment.cmd")
    helper.writeText("@echo off\r\ncall \"%RACHEL_SDL_VCVARS%\" >nul\r\nif errorlevel 1 exit /b %errorlevel%\r\nset\r\n")
    // Environment output can contain credentials. Capture it internally; never log it.
    val process = ProcessBuilder("cmd.exe", "/d", "/c", helper.absolutePath).apply {
        environment().putAll(base)
        environment()["RACHEL_SDL_VCVARS"] = vcvars.absolutePath
        redirectError(ProcessBuilder.Redirect.DISCARD)
    }.start()
    val encoding = java.nio.charset.Charset.forName(System.getProperty("native.encoding", java.nio.charset.Charset.defaultCharset().name()))
    val output = process.inputStream.bufferedReader(encoding).use { it.readText() }
    check(process.waitFor() == 0) { "Cannot initialize MSVC for static SDL" }
    val variables = base.toMutableMap()
    for (line in output.lineSequence()) {
        val separator = line.indexOf('=')
        if (separator > 0) variables[line.substring(0, separator)] = line.substring(separator + 1)
    }
    val ninja = File(installation, "Common7/IDE/CommonExtensions/Microsoft/CMake/Ninja")
    if (ninja.resolve("ninja.exe").isFile) variables["PATH"] = ninja.absolutePath + File.pathSeparator + variables["PATH"].orEmpty()
    return variables
}

fun Project.sdkLinuxOptions(nativeHome: File, variables: Map<String, String>, work: File): Pair<List<String>, Map<String, String>> {
    val properties = nativeHome.resolve("konan/konan.properties").readText()
    val identities = listOf("toolchainDependency.linux_x64", "llvm.linux_x64.user").associateWith { key ->
        Regex("(?m)^${Regex.escape(key)}[ \\t]*=[ \\t]*([A-Za-z0-9_.-]+)[ \\t]*$").find(properties)?.groupValues?.get(1)
            ?: error("Cannot resolve $key from the selected Kotlin/Native distribution")
    }
    val dependencies = File(variables["KONAN_DATA_DIR"] ?: File(System.getProperty("user.home"), ".konan").path, "dependencies")
    val toolchain = dependencies.resolve(identities.getValue("toolchainDependency.linux_x64"))
    val llvm = dependencies.resolve(identities.getValue("llvm.linux_x64.user"))
    val sysroot = toolchain.resolve("x86_64-unknown-linux-gnu/sysroot")
    val compiler = llvm.resolve("bin/clang")
    val ar = toolchain.resolve("bin/x86_64-unknown-linux-gnu-ar")
    val ranlib = toolchain.resolve("bin/x86_64-unknown-linux-gnu-ranlib")
    val required = listOf(compiler, ar, ranlib, sysroot.resolve("usr/include/features.h"))
    if (required.any { !it.isFile }) {
        work.mkdirs()
        val definition = work.resolve("toolchain.def")
        definition.writeText("headers = stdint.h\npackage = love.yinlin.nativecompose.toolchain\n")
        logger.lifecycle(sdkOutput(listOf(nativeHome.resolve("bin/cinterop").path, "-def", definition.path, "-target", "linux_x64", "-o", work.resolve("toolchain.klib").path), variables))
    }
    check(required.all { it.isFile }) { "Kotlin/Native Linux C toolchain is incomplete: ${required.filter { !it.isFile }}" }
    return listOf("-DCMAKE_C_COMPILER=$compiler", "-DCMAKE_C_COMPILER_TARGET=x86_64-unknown-linux-gnu", "-DCMAKE_C_COMPILER_EXTERNAL_TOOLCHAIN=$toolchain",
        "-DCMAKE_SYSROOT=$sysroot", "-DCMAKE_AR=$ar", "-DCMAKE_RANLIB=$ranlib", "-DCMAKE_C_FLAGS=-idirafter /usr/include -idirafter /usr/include/x86_64-linux-gnu") to identities
}

fun sdkLinkOptions(text: String): List<String> {
    // pkg-config quotes directory paths; only portable system dependencies reach consumers.
    val portable = text.replace(Regex("-L(?:\"[^\"]*\"|'[^']*'|[^\\s]+)"), "")
    val tokens = Regex("\"[^\"]*\"|'[^']*'|[^\\s]+").findAll(portable).map { it.value.trim('"', '\'') }.toList()
    val result = mutableListOf<String>()
    var directory = false
    for (token in tokens.flatMap { if (it.startsWith("-Wl,")) it.removePrefix("-Wl,").split(',') else listOf(it) }) {
        if (directory) {
            directory = false
            continue
        }
        if (token == "-L") {
            directory = true
            continue
        }
        if (token.startsWith("-L") || token in listOf("-Xlinker", "-lSDL3", "-lSDL3-static", "-mwindows")) continue
        check(!Regex("[A-Za-z]:[\\\\/]|/(?:Users|home|tmp|private|Volumes)/").containsMatchIn(token)) { "Non-portable SDL link dependency: $token" }
        result.add(if (token == "-pthread") "-lpthread" else token)
    }
    return result
}

val sdkNativeDirectory = rootProject.projectDir.resolve("../../../../..").canonicalFile.resolve("native-compose")
val sdkLockFile = sdkNativeDirectory.resolve("upstream/sdl.json")
val sdkLock = JsonSlurper().parse(sdkLockFile) as Map<*, *>
val sdkBuildProfile = sdkLock["build"] as Map<*, *>
val sdkCmakeProfile = sdkBuildProfile["cmake"] as Map<*, *>
check(sdkBuildProfile["dynamicApi"] == false && sdkCmakeProfile.all { (name, value) -> name is String && Regex("SDL_[A-Z0-9_]+").matches(name) && value is Boolean }) { "Invalid SDL window SDK profile" }
val sdkVersion = sdkLock["version"] as String
val sdkHost = HostManager.host.name
check(sdkHost in setOf("mingw_x64", "linux_x64", "macos_arm64")) { "Unsupported static SDL host: $sdkHost" }
val sdkSource = sdkNativeDirectory.resolve("vendor/sdl-source-$sdkVersion")
val sdkPrefix = sdkNativeDirectory.resolve("vendor/sdl-static/$sdkHost")
val sdkArchiveName = if (sdkHost == "mingw_x64") "SDL3-static.lib" else "libSDL3.a"
val sdkScript = sdkNativeDirectory.parentFile.resolve("build-logic/native-bridge/gradle/sdl-sdk.gradle.kts")
val sdkDropPatch = sdkSource.resolve(".rachel-drop-patch.json")
val sdkNativeLocation = layout.buildDirectory.file("konan.txt")

tasks.register("prepareSdlStatic") {
    group = "native setup"
    description = "Build the pinned SDL source as a static SDK for the Kotlin/Native host"
    inputs.files(sdkLockFile, sdkScript, sdkSource.resolve(".rachel-source-sha256"), sdkSource.resolve("src/dynapi/SDL_dynapi.h"), sdkDropPatch)
    inputs.property("host", sdkHost)
    outputs.files(sdkPrefix.resolve("rachel-sdl.json"), sdkPrefix.resolve("lib/$sdkArchiveName"))
    if (sdkHost == "linux_x64") {
        dependsOn("downloadKotlinNativeDistribution")
        inputs.file(sdkNativeLocation)
    }
    doLast {
        check(sdkSource.resolve("include/SDL3/SDL.h").isFile) { "Run :repository-conventions:prepareNativeSources to prepare the SDL source" }
        val dynamicApiHeader = sdkSource.resolve("src/dynapi/SDL_dynapi.h")
        check(dynamicApiHeader.readText().contains("#define SDL_DYNAMIC_API 0 /* Rachel:")) { "Prepare the direct-call SDL source profile before building its SDK" }
        val work = sdkNativeDirectory.resolve("vendor/build/sdl-static")
        val environment = if (sdkHost == "mingw_x64") project.sdkWindowsEnvironment(System.getenv(), work) else System.getenv()
        val source = sdkLock["source"] as Map<*, *>
        val dropPatchSha256 = (JsonSlurper().parse(sdkDropPatch) as Map<*, *>)["patchSha256"] as String
        val profile = mutableMapOf<String, Any>("version" to sdkVersion, "target" to sdkHost, "archive" to sdkArchiveName,
            "linkage" to "static", "buildType" to "Release", "sourceSha256" to source["sha256"]!!, "scriptSha256" to sdkDigest(sdkScript),
            "dynamicApiHeaderSha256" to sdkDigest(dynamicApiHeader), "dropPatchSha256" to dropPatchSha256, "profile" to sdkBuildProfile)
        val nativeOptions = if (sdkHost == "linux_x64") {
            val (options, identities) = project.sdkLinuxOptions(File(sdkNativeLocation.get().asFile.readText().trim()), environment, work.resolve("bootstrap"))
            profile["nativeToolchain"] = identities
            options
        } else emptyList()
        val profileFile = sdkPrefix.resolve("rachel-sdl.json")
        val previous = if (profileFile.isFile) JsonSlurper().parse(profileFile) as Map<*, *> else emptyMap<String, Any>()
        val existingArchive = sdkPrefix.resolve("lib/$sdkArchiveName")
        if (profile.all { (key, value) -> previous[key] == value } && existingArchive.isFile && previous["archiveSha256"] == sdkDigest(existingArchive)) return@doLast
        val build = work.resolve(if (sdkHost == "linux_x64") "linux_x64-konan" else sdkHost)
        val options = mutableListOf("-DCMAKE_INSTALL_PREFIX=$sdkPrefix", "-DCMAKE_INSTALL_LIBDIR=lib", "-DCMAKE_BUILD_TYPE=Release", "-DSDL_SHARED=OFF", "-DSDL_STATIC=ON",
            "-DCMAKE_POSITION_INDEPENDENT_CODE=ON", "-DSDL_TEST_LIBRARY=OFF", "-DSDL_TESTS=OFF", "-DSDL_EXAMPLES=OFF")
        options.addAll(sdkCmakeProfile.map { (name, value) -> "-D$name=${if (value == true) "ON" else "OFF"}" })
        options.addAll(nativeOptions)
        if (sdkHost == "mingw_x64") options.addAll(listOf("-G", "Ninja", "-DCMAKE_MSVC_RUNTIME_LIBRARY=MultiThreaded"))
        else if (sdkHost == "macos_arm64") options.addAll(listOf("-DCMAKE_OSX_ARCHITECTURES=arm64", "-DCMAKE_OSX_DEPLOYMENT_TARGET=12.0"))
        for (command in listOf(listOf("cmake", "-S", sdkSource.path, "-B", build.path) + options,
            listOf("cmake", "--build", build.path, "--config", "Release", "--parallel", "2"), listOf("cmake", "--install", build.path, "--config", "Release"))) {
            logger.lifecycle(project.sdkOutput(command, environment))
        }
        val archive = sdkPrefix.resolve("lib/$sdkArchiveName")
        check(archive.isFile && archive.inputStream().use { String(it.readNBytes(8), Charsets.US_ASCII) } == "!<arch>\n") { "CMake did not produce a static SDL archive: $archive" }
        val pcDirectory = sdkPrefix.resolve("lib/pkgconfig")
        val pc = pcDirectory.resolve("sdl3.pc").readText()
        val requirements = Regex("(?m)^Requires(?:\\.private)?:[ \\t]*(.*)$").findAll(pc).any { it.groupValues[1].isNotBlank() }
        val dependencies = if (requirements) {
            val variables = environment + ("PKG_CONFIG_PATH" to (pcDirectory.path + File.pathSeparator + environment["PKG_CONFIG_PATH"].orEmpty()))
            project.sdkOutput(listOf("pkg-config", "--static", "--libs", "sdl3"), variables)
        } else Regex("(?m)^Libs(?:\\.private)?:[ \\t]*(.*)$").findAll(pc).joinToString(" ") { it.groupValues[1] }
        profile["linkerOpts"] = sdkLinkOptions(dependencies)
        profile["archiveSha256"] = sdkDigest(archive)
        profileFile.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(profile)) + "\n")
    }
}
