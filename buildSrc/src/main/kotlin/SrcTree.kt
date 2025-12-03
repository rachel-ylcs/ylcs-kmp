import org.gradle.api.file.Directory

class BuildSrcNode(root: RootProjectNode) : Directory by root.dir("buildSrc")

class ConfigNode(root: RootProjectNode, c: Constants) : Directory by root.dir("config") {
    private val packages = dir("packages")
    val currentPackages = packages.dir("${c.platform}-${c.architecture}")

    val stability = file("stability.conf")
    val icon = file(when (c.platform) {
        BuildPlatform.Windows -> "icon.ico"
        BuildPlatform.Linux -> "icon.png"
        BuildPlatform.Mac -> "icon.icns"
    })
    val androidKey = file("androidKey.jks")
}

class DocsNode(root: RootProjectNode) : Directory by root.dir("docs")

class IosAppNode(root: RootProjectNode) : Directory by root.dir("iosApp") {
    val core = dir("core")
    val podfile = file("Podfile")
    val configuration = dir("Configuration")
    val configurationFile = configuration.file("Version.xcconfig")
}

class NativeNode(root: RootProjectNode) : Directory by root.dir("native") {
    val libs = dir("libs")
}

class OutputsNode(root: RootProjectNode) : Directory by root.dir("outputs")

class ScriptNode(root: RootProjectNode) : Directory by root.dir("script")

class CSNode(root: RootProjectNode) : Directory by root.dir("ylcs-app").dir("cs") {
    private val build = dir("build")
    val srcGenerated = build.dir("generated").dir("kotlin")
    val generatedLocalFile = srcGenerated.dir("love").dir("yinlin").file("Local.kt")
}

class SharedNode(root: RootProjectNode, c: Constants) : Directory by root.dir("ylcs-app").dir("shared") {
    private val proguard = dir("proguard")
    val commonR8Rule = proguard.file("R8Common.pro")
    val androidR8Rule = proguard.file("R8Android.pro")
    val desktopR8Rule = proguard.file("R8Desktop.pro")

    private val build = dir("build")
    val composeCompilerReport = build.dir("composeCompiler")

    val desktopWorkSpace = build.dir("desktopRun")
    val desktopOriginOutput = build.dir("compose").dir("binaries").dir("main-release").dir("app")
    val desktopLibOutput = root.outputs.dir(when (c.platform) {
        BuildPlatform.Windows -> "${c.app.name}/app"
        BuildPlatform.Linux -> "${c.app.name}/lib/app"
        BuildPlatform.Mac -> "${c.app.name}.app/Contents/app"
    })
    val desktopPackagesOutput = root.outputs.dir(when (c.platform) {
        BuildPlatform.Windows -> c.app.name
        BuildPlatform.Linux -> "${c.app.name}/bin"
        BuildPlatform.Mac -> "${c.app.name}.app/Contents/MacOS"
    })

    val webOriginOutput = build.dir("dist").dir("wasmJs").dir("productionExecutable")
    val webOutput = root.outputs.dir("web")
}

class AndroidAppNode(root: RootProjectNode, c: Constants): Directory by root.dir("ylcs-app").dir("androidApp") {
    private val build = dir("build")

    val originOutput = build.dir("outputs").dir("apk").dir("release").file("androidApp-release.apk")
}

class ServerNode(root: RootProjectNode, c: Constants) : Directory by root.dir("ylcs-app").dir("server") {
    val workspace = dir("build").dir("serverRun")
    val outputs = root.outputs
    val outputFile = outputs.file(c.server.outputName)
}

class ModManagerNode(root: RootProjectNode, c: Constants) : Directory by root.dir("ylcs-app").dir("mod-manager") {
    private val build = dir("build")
    val desktopOriginOutput = build.dir("compose").dir("binaries").dir("main-release").dir("app")
}

class RootProjectNode(root: Directory, c: Constants) : Directory by root {
    val buildSrc = BuildSrcNode(this)
    val config = ConfigNode(this, c)
    val docs = DocsNode(this)
    val iosApp = IosAppNode(this)
    val native = NativeNode(this)
    val outputs = OutputsNode(this)
    val script = ScriptNode(this)
    val cs = CSNode(this)
    val app = SharedNode(this, c)
    val androidApp = AndroidAppNode(this, c)
    val server = ServerNode(this, c)
    val modManager = ModManagerNode(this, c)
    val libsVersion = file("libs.version.toml")
    val license = file("LICENSE")
    val localProperties = file("local.properties")
    val readme = file("README.md")
}