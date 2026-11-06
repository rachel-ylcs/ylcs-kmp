import org.gradle.api.GradleException
import java.io.File
import java.util.Properties
import java.util.concurrent.TimeUnit

// Root Settings only. Avoid network, package managers, compilation and deep filesystem scans.
val env = System.getenv()
val os = System.getProperty("os.name").lowercase()
val arch = System.getProperty("os.arch").lowercase()
val win = os.startsWith("windows")
val lin = os.startsWith("linux")
val mac = os.startsWith("mac")
val exe = if (win) ".exe" else ""
fun report(name: String, value: String?, hint: String = "", required: Boolean = true) {
    if (value != null) println("[PASS] $name — $value")
    else if (!required) println("[WARN] $name — $hint")
    else {
        val message = "[ERROR] $name — $hint"
        println(message)
        throw GradleException(message)
    }
}
fun quick(vararg args: String): String? {
    val process = try { ProcessBuilder(*args).redirectErrorStream(true).start() } catch (_: Exception) { return null }
    if (!process.waitFor(3, TimeUnit.SECONDS)) { process.destroyForcibly(); return null }
    if (process.exitValue() != 0) return null
    return process.inputStream.bufferedReader().use { it.readLine()?.trim().orEmpty() }
}
fun locate(name: String, extra: List<File> = emptyList()): File? {
    val names = if (win) listOf("$name.exe", "$name.cmd", "$name.bat") else listOf(name)
    val fromPath = env["PATH"].orEmpty().split(File.pathSeparator).filter { it.isNotEmpty() }.flatMap { dir -> names.map { File(dir, it) } }
    return (fromPath + extra).firstOrNull { it.isFile && (win || it.canExecute()) }
}
fun command(name: String, hints: List<File> = emptyList()): String {
    val found = locate(name, hints)
    val version = quick(name, "--version")
    report(name, version?.let { "$it — ${found?.absolutePath ?: "当前 Gradle PATH"}" },
        found?.let { "已安装在 ${it.absolutePath}，但当前 Gradle 进程无法执行 $name；请配置可执行路径" } ?: "未找到可调用的 $name")
    return version!!
}

report("宿主平台", if (((win || lin) && arch in setOf("amd64", "x86_64")) || (mac && arch in setOf("arm64", "aarch64"))) "$os / $arch" else null,
    "不支持 $os / $arch；支持 Windows/Linux x64、macOS ARM64")
command("git", listOfNotNull(if (win) env["ProgramFiles"]?.let { File(it, "Git/cmd/git.exe") } else null, if (mac) File("/usr/bin/git") else null))

val props = Properties()
val local = settingsDir.resolve("local.properties")
if (local.isFile) local.inputStream().use(props::load)
val home = System.getProperty("user.home")
val sdk = listOfNotNull(props.getProperty("sdk.dir")?.let(::File), env["ANDROID_HOME"]?.let(::File), env["ANDROID_SDK_ROOT"]?.let(::File),
    if (win) env["LOCALAPPDATA"]?.let { File(it, "Android/Sdk") } else null,
    if (lin) File(home, "Android/Sdk") else null, if (mac) File(home, "Library/Android/sdk") else null)
    .firstOrNull { it.isDirectory && File(it, "platforms").isDirectory }
report("Android SDK", sdk?.let { "${it.absolutePath} (sdk.dir=${props.getProperty("sdk.dir") ?: "未设置"}, ANDROID_HOME=${env["ANDROID_HOME"] ?: "未设置"})" },
    "未找到有效 SDK；检查 local.properties 的 sdk.dir 或 ANDROID_HOME")
val android = sdk!!

val vs = if (win) {
    val candidates = listOfNotNull(env["VS_PATH"]?.let(::File), env["SKIKO_VSBT_PATH"]?.let(::File)) +
        listOf("18", "2022", "2026").flatMap { year -> listOf("Community", "Professional", "Enterprise", "BuildTools").map { File(env["ProgramFiles"].orEmpty(), "Microsoft Visual Studio/$year/$it") } }
    val found = candidates.firstOrNull { File(it, "VC/Auxiliary/Build/vcvars64.bat").isFile }
    report("Visual Studio C++", found?.absolutePath, "缺少 x64 MSVC C++ Build Tools")
    val vcvars = env["VS_PATH"]?.let { File(it, "VC/Auxiliary/Build/vcvars64.bat") }
    report("VS_PATH", vcvars?.takeIf { it.isFile }?.let { "${env["VS_PATH"]} -> ${it.absolutePath}" }, "当前 JNI 构建要求 VS_PATH 指向 ${found?.absolutePath ?: "Visual Studio 安装根目录"}")
    val kits = listOfNotNull(env["WindowsSdkDir"]?.let(::File), env["ProgramFiles(x86)"]?.let { File(it, "Windows Kits/10") })
    val kit = kits.firstOrNull { File(it, "Include").listFiles()?.any { ver -> File(ver, "ucrt").isDirectory && File(ver, "um").isDirectory } == true }
    report("Windows SDK", kit?.absolutePath, "缺少 Windows SDK 的 UCRT/UM 头文件")
    found
} else null
if (lin) report("Linux C++ 编译器", (locate("g++") ?: locate("clang++"))?.absolutePath, "未找到 g++/clang++；安装 Ubuntu C++ 工具链")
val xcode = if (mac) {
    val developer = env["DEVELOPER_DIR"]?.let(::File) ?: quick("/usr/bin/xcode-select", "-p")?.let(::File)
        ?: File("/Applications/Xcode.app/Contents/Developer")
    val sdkDir = File(developer, "Platforms/MacOSX.platform/Developer/SDKs")
    report("Xcode / macOS SDK", developer.takeIf { sdkDir.isDirectory && File("/usr/bin/xcrun").isFile }?.absolutePath,
        "未找到完整 Xcode/macOS SDK；当前 DEVELOPER_DIR=${developer.absolutePath}")
    developer
} else null

val cmakeHints = listOfNotNull(if (win) vs?.let { File(it, "Common7/IDE/CommonExtensions/Microsoft/CMake/CMake/bin/cmake.exe") } else null,
    if (mac) File("/opt/homebrew/bin/cmake") else null, if (mac) File("/Applications/CMake.app/Contents/bin/cmake") else null) +
    File(android, "cmake").listFiles().orEmpty().map { File(it, "bin/cmake$exe") }
val cmakeVersion = command("cmake", cmakeHints)
val version = Regex("cmake version (\\d+)\\.(\\d+)").find(cmakeVersion)?.groupValues
report("CMake >= 3.24", if (version != null && (version[1].toInt() > 3 || version[1].toInt() == 3 && version[2].toInt() >= 24)) cmakeVersion else null,
    "检测到 $cmakeVersion；本仓库的 CMake 脚本需要 >= 3.24")
val ninjaHint = if (win) vs?.let { File(it, "Common7/IDE/CommonExtensions/Microsoft/CMake/Ninja/ninja.exe") } else null
val ninja = locate("ninja", listOfNotNull(ninjaHint))
val ninjaVersion = quick("ninja", "--version")
report("Ninja", ninjaVersion?.let { "$it — ${ninja?.absolutePath ?: "当前 Gradle PATH"}" }
    ?: if (win) ninja?.absolutePath else null, "找不到 CMake 可用的 Ninja 构建器")

for ((name, relative) in listOf("MMKV 子模块" to "ylcs-module/foundation/kv/src/nativeMain/cpp/mmkv/Core/CMakeLists.txt",
    "miniaudio 子模块" to "ylcs-module/compose/components/media/src/nativeMain/cpp/miniaudio/miniaudio.c")) {
    val source = settingsDir.resolve(relative)
    report(name, source.takeIf { it.isFile }?.parentFile?.absolutePath, "缺少 ${source.absolutePath}；执行 git submodule update --init --recursive", false)
}
val platform = File(android, "platforms/android-37.1")
report("Android SDK Platform 37.1", platform.takeIf { it.isDirectory }?.absolutePath, "未安装 ${platform.absolutePath}；Android 构建可能需要补装", false)
val ndk = File(android, "ndk/29.0.14206865")
report("Android NDK 29.0.14206865", ndk.takeIf { File(it, "source.properties").isFile }?.absolutePath, "未安装 ${ndk.absolutePath}；原生构建可能需要补装", false)
val adb = File(android, "platform-tools/adb$exe")
report("Android ADB", adb.takeIf { it.isFile }?.absolutePath, "缺少 ${adb.absolutePath}；真机运行可能受限", false)
val chrome = locate("google-chrome", listOfNotNull(env["CHROME_BIN"]?.let(::File),
    if (win) env["ProgramFiles"]?.let { File(it, "Google/Chrome/Application/chrome.exe") } else null,
    if (mac) File("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome") else null))
    ?: locate("chromium") ?: locate("chromium-browser")
report("Chrome / Chromium", chrome?.absolutePath, "未找到浏览器；Web ChromeHeadless 测试可能不可用", false)
if (mac) {
    val simulator = File(xcode!!, "Platforms/iPhoneSimulator.platform/Developer/SDKs")
    report("iOS Simulator SDK", simulator.takeIf { it.isDirectory }?.absolutePath, "缺少 ${simulator.absolutePath}；模拟器运行可能受限", false)
}
