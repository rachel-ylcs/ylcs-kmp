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

class DesignAENode(root: RootProjectNode) : Directory by root.dir("design-AE")

class DesignPSNode(root: RootProjectNode) : Directory by root.dir("design-PS")

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

class AppNode(root: RootProjectNode, c: Constants) : Directory by root.dir("ylcs-app") {
    private val proguard = dir("proguard")
    val commonR8Rule = proguard.file("R8Common.pro")
    val androidR8Rule = proguard.file("R8Android.pro")
    val desktopR8Rule = proguard.file("R8Desktop.pro")

    private val build = dir("build")
    val composeCompilerReport = build.dir("composeCompiler")

    val androidOriginOutput = build.dir("outputs").dir("apk").dir("release").file("${c.app.projectName}-release.apk")

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

class ModManagerNode(root: RootProjectNode) : Directory by root.dir("ylcs-modManager")

class MusicNode(root: RootProjectNode) : Directory by root.dir("ylcs-music")

class ServerNode(root: RootProjectNode, c: Constants) : Directory by root.dir("ylcs-server") {
    val workspace = dir("build").dir("serverRun")
    val outputs = root.outputs
    val outputFile = outputs.file(c.server.outputName)
}

class SharedNode(root: RootProjectNode) : Directory by root.dir("ylcs-shared") {
    private val build = dir("build")
    val srcGenerated = build.dir("generated").dir("kotlin")
    val generatedLocalFile = srcGenerated.dir("love").dir("yinlin").file("Local.kt")
}

class RootProjectNode(root: Directory, c: Constants) : Directory by root {
    val buildSrc = BuildSrcNode(this)
    val config = ConfigNode(this, c)
    val designAE = DesignAENode(this)
    val designPS = DesignPSNode(this)
    val docs = DocsNode(this)
    val iosApp = IosAppNode(this)
    val native = NativeNode(this)
    val outputs = OutputsNode(this)
    val script = ScriptNode(this)
    val app = AppNode(this, c)
    val modManager = ModManagerNode(this)
    val music = MusicNode(this)
    val server = ServerNode(this, c)
    val shared = SharedNode(this)
    val libsVersion = file("libs.version.toml")
    val license = file("LICENSE")
    val localProperties = file("local.properties")
    val readme = file("README.md")
}