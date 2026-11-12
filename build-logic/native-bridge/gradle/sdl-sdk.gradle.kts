import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import java.security.MessageDigest
import java.nio.file.Files

fun sdkDigest(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input -> java.security.DigestInputStream(input, digest).use { it.transferTo(java.io.OutputStream.nullOutputStream()) } }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun Project.sdkOutput(arguments: List<String>, variables: Map<String, String>): String {
    val result = providers.exec {
        commandLine(arguments)
        setEnvironment(variables)
        isIgnoreExitValue = true
    }
    val output = result.standardOutput.asText.get()
    check(result.result.get().exitValue == 0) { "SDL SDK command failed: ${arguments.first()}\n${result.standardError.asText.get()}\n$output" }
    return output
}

fun sdkTool(variables: Map<String, String>, name: String): File? =
    variables["PATH"].orEmpty().split(File.pathSeparator).asSequence().map { it.trim().trim('"') }
        .filter { it.isNotEmpty() }.map { File(it, name) }.firstOrNull { it.isFile }?.canonicalFile

fun Project.sdkWindowsEnvironment(base: Map<String, String>, work: File): Map<String, String> {
    val variables = base.mapKeys { it.key.uppercase(java.util.Locale.ROOT) }
    fun ready(environment: Map<String, String>): Boolean {
        val compiler = sdkTool(environment, "cl.exe") ?: return false
        val linker = sdkTool(environment, "link.exe") ?: return false
        return !environment["INCLUDE"].isNullOrBlank() && !environment["LIB"].isNullOrBlank() &&
            environment["VSCMD_ARG_TGT_ARCH"].orEmpty().lowercase() in setOf("", "x64", "amd64") &&
            compiler.parentFile.name.equals("x64", ignoreCase = true) && linker.parentFile == compiler.parentFile
    }
    if (ready(variables) && sdkTool(variables, "ninja.exe") != null && sdkTool(variables, "cmake.exe") != null) return variables
    val explicit = listOf("SKIKO_VSBT_PATH", "VS_PATH", "VSINSTALLDIR").firstNotNullOfOrNull { variables[it]?.takeIf(String::isNotBlank)?.let(::File) }
    val vswhere = variables["PROGRAMFILES(X86)"]?.let { File(it, "Microsoft Visual Studio/Installer/vswhere.exe") }
    val installation = explicit ?: vswhere?.takeIf(File::isFile)?.let {
        val path = sdkOutput(listOf(it.path, "-latest", "-products", "*", "-requires",
            "Microsoft.VisualStudio.Component.VC.Tools.x86.x64", "-property", "installationPath"), variables).trim()
        path.takeIf(String::isNotEmpty)?.let(::File)
    }
    check(installation != null) { "Cannot locate x64 MSVC for SDL; install the Visual Studio C++ tools or set VS_PATH to the installation directory." }
    val environment = variables.toMutableMap()
    if (!ready(variables)) {
        val vcvars = installation.resolve("VC/Auxiliary/Build/vcvars64.bat")
        check(vcvars.isFile) { "Missing MSVC environment script: $vcvars" }
        val helper = Files.createTempFile(work.toPath(), "vcvars-", ".cmd").toFile()
        try {
            helper.writeText("@echo off\r\ncall \"%RACHEL_SDL_VCVARS%\" >nul\r\nif errorlevel 1 exit /b %errorlevel%\r\nset\r\n")
            // Capture the environment privately, preserving Windows' case-insensitive variable names.
            val process = ProcessBuilder("cmd.exe", "/d", "/c", helper.name).directory(work).apply {
                environment().clear()
                environment().putAll(variables.filterKeys { name ->
                    name !in setOf("INCLUDE", "LIB", "LIBPATH", "VCTOOLSINSTALLDIR", "VCTOOLSVERSION", "WINDOWSSDKDIR", "WINDOWSSDKVERSION") &&
                        !name.startsWith("VSCMD_") && !name.startsWith("__VSCMD_")
                })
                variables["__VSCMD_PREINIT_PATH"]?.takeIf(String::isNotBlank)?.let { environment()["PATH"] = it }
                environment()["RACHEL_SDL_VCVARS"] = vcvars.absolutePath
                redirectError(ProcessBuilder.Redirect.DISCARD)
            }.start()
            val encoding = java.nio.charset.Charset.forName(System.getProperty("native.encoding", java.nio.charset.Charset.defaultCharset().name()))
            val output = process.inputStream.bufferedReader(encoding).use { it.readText() }
            check(process.waitFor() == 0) { "Cannot initialize x64 MSVC using $vcvars" }
            environment.clear()
            for (line in output.lineSequence()) {
                val separator = line.indexOf('=')
                if (separator > 0) environment[line.substring(0, separator).uppercase(java.util.Locale.ROOT)] = line.substring(separator + 1)
            }
            environment.remove("RACHEL_SDL_VCVARS")
        }
        finally { helper.delete() }
    }
    check(ready(environment)) { "MSVC initialization did not supply cl.exe, link.exe, INCLUDE and LIB for x64: $installation" }
    val ninja = installation.resolve("Common7/IDE/CommonExtensions/Microsoft/CMake/Ninja")
    val cmake = installation.resolve("Common7/IDE/CommonExtensions/Microsoft/CMake/CMake/bin")
    environment["PATH"] = (listOf(ninja, cmake).filter(File::isDirectory).map { it.absolutePath } + environment["PATH"].orEmpty()).joinToString(File.pathSeparator)
    return environment
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
        "-DCMAKE_SYSROOT=$sysroot", "-DCMAKE_AR=$ar", "-DCMAKE_RANLIB=$ranlib", "-DCMAKE_C_FLAGS=-idirafter /usr/include -idirafter /usr/include/x86_64-linux-gnu -w") to identities
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

val sdkGeneration = rootProject.projectDir.resolve("../../..").canonicalFile
val sdkNativeDirectory = sdkGeneration.resolve("../../..").canonicalFile
val sdkMetadataFile = sdkGeneration.resolve("metadata/sdl.json")
val sdkMetadata = JsonSlurper().parse(sdkMetadataFile) as Map<*, *>
val sdkBuildProfile = sdkMetadata["build"] as Map<*, *>
val sdkCmakeProfile = sdkBuildProfile["cmake"] as Map<*, *>
check(sdkBuildProfile["dynamicApi"] == false && sdkCmakeProfile.all { (name, value) -> name is String && Regex("SDL_[A-Z0-9_]+").matches(name) && value is Boolean }) { "Invalid SDL window SDK profile" }
val sdkVersion = sdkMetadata["version"] as String
val sdkHost = HostManager.host.name
check(sdkHost in setOf("mingw_x64", "linux_x64", "macos_arm64")) { "Unsupported static SDL host: $sdkHost" }
val sdkSource = sdkGeneration.resolve("sdl/source")
val sdkPrefix = sdkGeneration.resolve("sdl/sdk/$sdkHost")
extensions.extraProperties["rachel.sdl.sdk"] = sdkPrefix
val sdkArchiveName = if (sdkHost == "mingw_x64") "SDL3-static.lib" else "libSDL3.a"
val sdkScript = sdkNativeDirectory.parentFile.resolve("build-logic/native-bridge/gradle/sdl-sdk.gradle.kts")
val sdkSourceRecord = sdkGeneration.resolve("sdl/files.json")
val sdkNativeLocation = layout.buildDirectory.file("konan.txt")

tasks.register("prepareSdlStatic") {
    group = "native setup"
    description = "Build the selected SDL source as a static SDK for the Kotlin/Native host"
    inputs.files(sdkMetadataFile, sdkScript, sdkSourceRecord, sdkGeneration.resolve(".rachel-generation.json"))
    inputs.property("host", sdkHost)
    outputs.dir(sdkPrefix)
    // Always verify the installed archive and toolchain identity before reusing the SDK.
    outputs.upToDateWhen { false }
    outputs.doNotCacheIf("The static SDK is verified against the resolved host toolchain") { true }
    if (sdkHost == "linux_x64") {
        dependsOn("downloadKotlinNativeDistribution")
        inputs.file(sdkNativeLocation)
    }
    doLast {
        check(sdkSource.resolve("include/SDL3/SDL.h").isFile) { "Run :repository-conventions:prepareNativeSources to prepare the SDL source" }
        val dynamicApiHeader = sdkSource.resolve("src/dynapi/SDL_dynapi.h")
        check(dynamicApiHeader.readText().contains("#define SDL_DYNAMIC_API 0 /* Rachel:")) { "Prepare the direct-call SDL source profile before building its SDK" }
        val work = sdkGeneration.resolve("sdl/cmake/$sdkHost").apply { mkdirs() }
        val environment = if (sdkHost == "mingw_x64") project.sdkWindowsEnvironment(System.getenv(), work) else System.getenv()
        val source = sdkMetadata["source"] as Map<*, *>
        val profile = mutableMapOf<String, Any>("version" to sdkVersion, "target" to sdkHost, "archive" to sdkArchiveName,
            "linkage" to "static", "buildType" to "Release", "sourceSha256" to source["sha256"]!!, "scriptSha256" to sdkDigest(sdkScript),
            "preparedSourceSha256" to sdkDigest(sdkSourceRecord), "profile" to sdkBuildProfile)
        val cmake = if (sdkHost == "mingw_x64") {
            checkNotNull(sdkTool(environment, "cmake.exe")) { "Cannot find cmake.exe in the SDL build environment." }.path
        } else "cmake"
        val nativeOptions = when (sdkHost) {
            "linux_x64" -> {
                val (options, identities) = project.sdkLinuxOptions(File(sdkNativeLocation.get().asFile.readText().trim()), environment, work.resolve("bootstrap"))
                profile["nativeToolchain"] = identities
                options
            }
            "mingw_x64" -> {
                val compiler = checkNotNull(sdkTool(environment, "cl.exe"))
                val linker = checkNotNull(sdkTool(environment, "link.exe"))
                val ninja = checkNotNull(sdkTool(environment, "ninja.exe")) { "Cannot find ninja.exe in the SDL build environment." }
                profile["nativeToolchain"] = mapOf("compiler" to compiler.path, "compilerSha256" to sdkDigest(compiler),
                    "windowsSdk" to environment["WINDOWSSDKVERSION"].orEmpty(), "ninjaSha256" to sdkDigest(ninja), "cmakeSha256" to sdkDigest(File(cmake)))
                listOf("-G", "Ninja", "-DCMAKE_C_COMPILER=${compiler.path}", "-DCMAKE_CXX_COMPILER=${compiler.path}",
                    "-DCMAKE_LINKER=${linker.path}", "-DCMAKE_MAKE_PROGRAM=${ninja.path}", "-DCMAKE_MSVC_RUNTIME_LIBRARY=MultiThreaded")
            }
            else -> listOf("-DCMAKE_OSX_ARCHITECTURES=arm64", "-DCMAKE_OSX_DEPLOYMENT_TARGET=12.0")
        }
        val profileFile = sdkPrefix.resolve("rachel-sdl.json")
        val previous = if (profileFile.isFile) JsonSlurper().parse(profileFile) as Map<*, *> else emptyMap<String, Any>()
        val existingArchive = sdkPrefix.resolve("lib/$sdkArchiveName")
        if (profile.all { (key, value) -> previous[key] == value } && existingArchive.isFile && previous["archiveSha256"] == sdkDigest(existingArchive)) return@doLast
        // A failed or changed compiler probe must never leave a CMakeCache for the next attempt.
        val build = Files.createTempDirectory(work.toPath(), "configure-").toFile()
        val options = mutableListOf("-DCMAKE_INSTALL_PREFIX=$sdkPrefix", "-DCMAKE_INSTALL_LIBDIR=lib", "-DCMAKE_BUILD_TYPE=Release", "-DSDL_SHARED=OFF", "-DSDL_STATIC=ON",
            "-DCMAKE_POSITION_INDEPENDENT_CODE=ON", "-DSDL_TEST_LIBRARY=OFF", "-DSDL_TESTS=OFF", "-DSDL_EXAMPLES=OFF")
        val warningFlag = if (sdkHost == "mingw_x64") "/w" else "-w"
        options.addAll(listOf("-DCMAKE_C_FLAGS_INIT=$warningFlag", "-DCMAKE_CXX_FLAGS_INIT=$warningFlag"))
        options.addAll(sdkCmakeProfile.map { (name, value) -> "-D$name=${if (value == true) "ON" else "OFF"}" })
        options.addAll(nativeOptions)
        try {
            for (command in listOf(listOf(cmake, "-S", sdkSource.path, "-B", build.path) + options,
                listOf(cmake, "--build", build.path, "--config", "Release", "--parallel", "2"), listOf(cmake, "--install", build.path, "--config", "Release"))) {
                logger.lifecycle(project.sdkOutput(command, environment))
            }
        }
        finally {
            check(build.canonicalFile.parentFile == work.canonicalFile && !Files.isSymbolicLink(build.toPath()))
            Files.walk(build.toPath()).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { Files.delete(it) }
            }
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
