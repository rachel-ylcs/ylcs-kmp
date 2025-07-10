import org.gradle.api.file.Directory

open class TreeNode(path: Directory) : Directory by path {
    override fun toString(): String = asFile.absolutePath
}

class BuildSrcNode(parent: RootProjectNode) : TreeNode(parent.dir("buildSrc")) {

}

class ConfigNode(c: Constants, parent: RootProjectNode) : TreeNode(parent.dir("config")) {
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

class DesignAENode(parent: RootProjectNode) : TreeNode(parent.dir("design-AE")) {

}

class DesignPSNode(parent: RootProjectNode) : TreeNode(parent.dir("design-PS")) {

}

class DocsNode(parent: RootProjectNode) : TreeNode(parent.dir("docs")) {

}

class IosAppNode(parent: RootProjectNode) : TreeNode(parent.dir("iosApp")) {
    val core = dir("core")
    val podfile = file("Podfile")
    val configuration = dir("Configuration")
    val configurationFile = configuration.file("Version.xcconfig")
}

class NativeNode(parent: RootProjectNode) : TreeNode(parent.dir("native")) {
    val libs = dir("libs")
}

class OutputsNode(parent: RootProjectNode) : TreeNode(parent.dir("outputs")) {

}

class ScriptNode(parent: RootProjectNode) : TreeNode(parent.dir("script")) {

}

class AppNode(c: Constants, parent: RootProjectNode) : TreeNode(parent.dir("ylcs-app")) {
    private val proguard = dir("proguard")
    val commonR8Rule = proguard.file("R8Common.pro")
    val androidR8Rule = proguard.file("R8Android.pro")
    val desktopR8Rule = proguard.file("R8Desktop.pro")

    private val build = dir("build")
    val composeCompilerReport = build.dir("composeCompiler")

    val androidOriginOutput = build.dir("outputs").dir("apk").dir("release").file("${c.app.projectName}-release.apk")

    val desktopWorkSpace = build.dir("desktopRun")
    val desktopOriginOutput = build.dir("compose").dir("binaries").dir("main-release").dir("app")
    val desktopLibOutput = parent.outputs.dir(when (c.platform) {
        BuildPlatform.Windows -> "${c.app.name}/app"
        BuildPlatform.Linux -> "${c.app.name}/lib/app"
        BuildPlatform.Mac -> "${c.app.name}.app/Contents/app"
    })
    val desktopPackagesOutput = parent.outputs.dir(when (c.platform) {
        BuildPlatform.Windows -> c.app.name
        BuildPlatform.Linux -> "${c.app.name}/bin"
        BuildPlatform.Mac -> "${c.app.name}.app/Contents/MacOS"
    })

    val webOriginOutput = build.dir("dist").dir("wasmJs").dir("productionExecutable")
    val webOutput = parent.outputs.dir("web")
}

class ModManagerNode(parent: RootProjectNode) : TreeNode(parent.dir("ylcs-modManager")) {

}

class MusicNode(parent: RootProjectNode) : TreeNode(parent.dir("ylcs-music")) {

}

class ServerNode(c: Constants, parent: RootProjectNode) : TreeNode(parent.dir("ylcs-server")) {
    val workspace = dir("build").dir("serverRun")
    val outputs = parent.outputs
    val outputFile = outputs.file(c.server.outputName)
}

class SharedNode(parent: RootProjectNode) : TreeNode(parent.dir("ylcs-shared")) {
    private val build = dir("build")
    val srcGenerated = build.dir("generated").dir("kotlin")
    val generatedLocalFile = srcGenerated.dir("love").dir("yinlin").file("Local.kt")
}

class RootProjectNode(c: Constants, root: Directory) : TreeNode(root) {
    val buildSrc = BuildSrcNode(this)
    val config = ConfigNode(c, this)
    val designAE = DesignAENode(this)
    val designPS = DesignPSNode(this)
    val docs = DocsNode(this)
    val iosApp = IosAppNode(this)
    val native = NativeNode(this)
    val outputs = OutputsNode(this)
    val script = ScriptNode(this)
    val app = AppNode(c, this)
    val modManager = ModManagerNode(this)
    val music = MusicNode(this)
    val server = ServerNode(c, this)
    val shared = SharedNode(this)
    val libsVersion = file("libs.version.toml")
    val license = file("LICENSE")
    val localProperties = file("local.properties")
    val readme = file("README.md")
}