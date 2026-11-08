import org.gradle.api.GradleException
import java.io.File
import java.util.Properties
import java.util.concurrent.TimeUnit

val env = System.getenv()!!
val os = System.getProperty("os.name").lowercase()
val arch = System.getProperty("os.arch").lowercase()
val win = os.startsWith("windows")
val linux = os.startsWith("linux")
val mac = os.startsWith("mac")
val exe = if (win) ".exe" else ""
val reports = mutableListOf<String>()

fun report(name: String, value: String?, hint: String = "", required: Boolean = true) {
    if (value != null) reports.add("[PASS] $name — $value")
    else if (!required) reports.add("[WARN] $name — $hint")
    else throw GradleException("[ERROR] $name — $hint")
}

fun quickOutput(vararg args: String): String? {
    val process = try { ProcessBuilder(*args).redirectErrorStream(true).start() } catch (_: Exception) { return null }
    if (!process.waitFor(3, TimeUnit.SECONDS)) { process.destroyForcibly(); return null }
    if (process.exitValue() != 0) return null
    return process.inputStream.bufferedReader().use { it.readText().trim() }
}

fun quick(vararg args: String): String? = quickOutput(*args)?.lineSequence()?.firstOrNull()?.trim()

fun pathDirectories(): List<File> = System.getenv("PATH").orEmpty().split(File.pathSeparator)
    .map { it.trim().trim('"') }.filter(String::isNotEmpty).map(::File)

fun locate(name: String, extra: List<File> = emptyList()): File? {
    val names = if (win) listOf("$name.exe", "$name.cmd", "$name.bat") else listOf(name)
    val fromPath = pathDirectories().flatMap { dir -> names.map(dir::resolve) }
    return (fromPath + extra).firstOrNull { it.isFile && (win || it.canExecute()) }
}

fun windowsCmakeRoots(): List<File> {
    if (!win) return emptyList()
    val hints = listOf("VS_PATH", "VSINSTALLDIR", "SKIKO_VSBT_PATH").mapNotNull { name ->
        System.getenv(name)?.trim()?.trim('"')?.takeIf(String::isNotEmpty)?.let(::File)
    }
    val programDirectories = listOfNotNull(System.getenv("ProgramFiles(x86)"), System.getenv("ProgramFiles"), System.getenv("ProgramW6432")).distinct()
    val vswhere = programDirectories.asSequence().map { File(it, "Microsoft Visual Studio/Installer/vswhere.exe") }.firstOrNull(File::isFile)
    val registered = vswhere?.let { quickOutput(it.absolutePath, "-all", "-products", "*", "-property", "installationPath", "-utf8") }
        .orEmpty().lineSequence().map(String::trim).filter(String::isNotEmpty).map(::File).toList()
    val standard = programDirectories.flatMap { File(it, "Microsoft Visual Studio").listFiles().orEmpty().filter(File::isDirectory) }
        .flatMap { it.listFiles().orEmpty().filter(File::isDirectory) }
    return (hints + registered + standard).map { it.resolve("Common7/IDE/CommonExtensions/Microsoft/CMake") }.distinct()
}

fun nativeTool(name: String, hints: List<File>, versionPrefix: String = ""): Pair<File, String> {
    val candidates = (pathDirectories().map { it.resolve("$name$exe") } + hints).filter(File::isFile).distinct()
    for (candidate in candidates) {
        val executable = candidate.canonicalFile
        val output = quickOutput(executable.path, "--version") ?: continue
        val version = output.lineSequence().map(String::trim)
            .firstOrNull { it.isNotEmpty() && it.startsWith(versionPrefix) } ?: continue
        report(name, "$version — ${executable.path}")
        return executable to version
    }
    val hint = if (candidates.isEmpty()) "未在 PATH 或已安装的工具目录中找到 $name" else "无法执行 ${candidates.joinToString { it.absolutePath }}"
    throw GradleException("[ERROR] $name — $hint")
}

fun command(name: String, hints: List<File> = emptyList()): String {
    val found = locate(name, hints)
    val version = quick(name, "--version")
    report(name, version?.let { "$it — ${found?.absolutePath ?: "当前 Gradle PATH"}" }, found?.let { "已安装在 ${it.absolutePath}，但当前 Gradle 进程无法执行 $name；请配置可执行路径" } ?: "未找到可调用的 $name")
    return version!!
}

// # 1 : platform
report("platform", if (((win || linux) && arch in setOf("amd64", "x86_64")) || (mac && arch in setOf("arm64", "aarch64"))) "$os / $arch" else null, "不支持 $os / $arch；支持 Windows/Linux x64、macOS ARM64")

// # 2 : git
command("git", listOfNotNull(
    if (win) env["ProgramFiles"]?.let { File(it, "Git/cmd/git.exe") } else null,
    if (mac) File("/usr/bin/git") else null)
)

val props = Properties()
val local = settingsDir.resolve("local.properties")
if (local.isFile) local.inputStream().use(props::load)
val home = System.getProperty("user.home")

// # 3 : Android SDK
val sdk = listOfNotNull(
    props.getProperty("sdk.dir")?.let(::File),
    env["ANDROID_HOME"]?.let(::File),
    env["ANDROID_SDK_ROOT"]?.let(::File),
    if (win) env["LOCALAPPDATA"]?.let { File(it, "Android/Sdk") } else null,
    if (linux) File(home, "Android/Sdk") else null,
    if (mac) File(home, "Library/Android/sdk") else null
).firstOrNull { it.isDirectory && File(it, "platforms").isDirectory }
report("Android SDK", sdk?.let { "${it.absolutePath} (sdk.dir=${props.getProperty("sdk.dir") ?: "未设置"}, ANDROID_HOME=${env["ANDROID_HOME"] ?: "未设置"})" }, "未找到有效 SDK；检查 local.properties 的 sdk.dir 或 ANDROID_HOME")
val android = sdk!!

// # 4-2 : g++ / clang ++
if (linux) report("Linux C++ 编译器", (locate("g++") ?: locate("clang++"))?.absolutePath, "未找到 g++/clang++；安装 Ubuntu C++ 工具链")

// # 4-3 : xcode / macOS SDK
val xcode = if (mac) {
    val developer = env["DEVELOPER_DIR"]?.let(::File) ?: quick("/usr/bin/xcode-select", "-p")?.let(::File) ?: File("/Applications/Xcode.app/Contents/Developer")
    val sdkDir = File(developer, "Platforms/MacOSX.platform/Developer/SDKs")
    report("Xcode / macOS SDK", developer.takeIf { sdkDir.isDirectory && File("/usr/bin/xcrun").isFile }?.absolutePath, "未找到完整 Xcode/macOS SDK；当前 DEVELOPER_DIR=${developer.absolutePath}")
    developer
} else null

// # 5 : cmake
val windowsCmake = windowsCmakeRoots()
val androidCmake = File(android, "cmake").listFiles().orEmpty().filter(File::isDirectory).sortedByDescending { it.name }
val cmakeHints = listOfNotNull(
    if (mac) File("/opt/homebrew/bin/cmake") else null,
    if (mac) File("/Applications/CMake.app/Contents/bin/cmake") else null
) + windowsCmake.map { it.resolve("CMake/bin/cmake.exe") } + androidCmake.map { it.resolve("bin/cmake$exe") }
val (cmake, cmakeVersion) = nativeTool("cmake", cmakeHints, versionPrefix = "cmake version ")
val version = Regex("cmake version (\\d+)\\.(\\d+)").find(cmakeVersion)?.groupValues

// # 6 : cmake version
report("CMake >= 3.24", if (version != null && (version[1].toInt() > 3 || version[1].toInt() == 3 && version[2].toInt() >= 24)) cmakeVersion else null, "检测到 $cmakeVersion；本仓库的 CMake 脚本需要 >= 3.24")

// # 7 : ninja
val ninja = nativeTool("ninja", windowsCmake.map { it.resolve("Ninja/ninja.exe") } + androidCmake.map { it.resolve("bin/ninja$exe") }).first

// # 8 : Submodules
for ((name, relative) in listOf(
    "MMKV 子模块" to "ylcs-module/foundation/kv/src/nativeMain/cpp/mmkv/Core/CMakeLists.txt",
    "miniaudio 子模块" to "ylcs-module/compose/components/media/src/nativeMain/cpp/miniaudio/miniaudio.c"
)) {
    val source = settingsDir.resolve(relative)
    report(name, source.takeIf { it.isFile }?.parentFile?.absolutePath, "缺少 ${source.absolutePath}；执行 git submodule update --init --recursive", false)
}

// # 9 : Android Platform Tools
val platform = File(android, "platforms/android-37.1")
report("Android SDK Platform 37.1", platform.takeIf { it.isDirectory }?.absolutePath, "未安装 ${platform.absolutePath}；Android 构建可能需要补装", false)

// # 10 : Android NDK
val ndk = File(android, "ndk/29.0.14206865")
report("Android NDK 29.0.14206865", ndk.takeIf { File(it, "source.properties").isFile }?.absolutePath, "未安装 ${ndk.absolutePath}；原生构建可能需要补装", false)

// # 11 : Android ADB
val adb = File(android, "platform-tools/adb$exe")
report("Android ADB", adb.takeIf { it.isFile }?.absolutePath, "缺少 ${adb.absolutePath}；真机运行可能受限", false)

// # 12 : Chrome
val chrome = locate("google-chrome", listOfNotNull(
    env["CHROME_BIN"]?.let(::File),
    if (win) env["ProgramFiles"]?.let { File(it, "Google/Chrome/Application/chrome.exe") } else null,
    if (mac) File("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome") else null)
) ?: locate("chromium") ?: locate("chromium-browser")
report("Chrome / Chromium", chrome?.absolutePath, "未找到浏览器；Web ChromeHeadless 测试可能不可用", false)

// # 13 : iOS Simulator
if (mac) {
    val simulator = File(xcode!!, "Platforms/iPhoneSimulator.platform/Developer/SDKs")
    report("iOS Simulator SDK", simulator.takeIf { it.isDirectory }?.absolutePath, "缺少 ${simulator.absolutePath}；模拟器运行可能受限", false)
}

gradle.extensions.extraProperties.set("rachel.native.cmakeExecutable", cmake.path)
gradle.extensions.extraProperties.set("rachel.native.ninjaExecutable", ninja.path)
println(reports.joinToString("\n"))
