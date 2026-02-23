package love.yinlin.project

import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile

class BuildSrcNode(root: RootProjectNode) : Directory by root.dir("buildSrc")

class ConfigNode(root: RootProjectNode, c: Constants) : Directory by root.dir("config") {
    val icon: RegularFile = file(when (c.platform) {
        BuildPlatform.Windows -> "icon.ico"
        BuildPlatform.Linux -> "icon.png"
        BuildPlatform.Mac -> "icon.icns"
    })
    val secret: Directory = dir("secret")
    val androidKey: RegularFile = secret.file("androidKey.jks")
}

class DocsNode(root: RootProjectNode) : Directory by root.dir("ylcs-docs") {
    val mkdocs: RegularFile = file("mkdocs.yml")
    val docs: Directory = dir("docs")
    val overrides: Directory = docs.dir("overrides")
    val dokka: Directory = overrides.dir("dokka")
    val gallery: Directory = overrides.dir("gallery")
}

class OutputsNode(root: RootProjectNode) : Directory by root.dir("outputs")

class ScriptNode(root: RootProjectNode) : Directory by root.dir("script")

class ArtifactsNode(root: RootProjectNode) : Directory by root.dir("artifacts") {
    val include: Directory = dir("include")
    val desktopNative: Directory = dir("desktopNative")
}

class WorkNode(root: RootProjectNode) : Directory by root.dir("work") {
    val desktop: Directory = dir("desktop")
    val server: Directory = dir("server")
    val modManager: Directory = dir("modManager")
}

class AndroidAppNode(root: RootProjectNode, c: Constants) : Directory by root.dir("ylcs-app").dir("androidApp") {
    private val build: Directory = dir("build")

    val originOutput: RegularFile = build.dir("outputs").dir("apk").dir("release").file("androidApp-release.apk")
}

class IosAppNode(root: RootProjectNode, c: Constants) : Directory by root.dir("ylcs-app").dir("iosApp") {
    val core: Directory = dir("core")
    val podfile: RegularFile = file("Podfile")
    val configuration: Directory = dir("Configuration")
    val configurationFile: RegularFile = configuration.file("Version.xcconfig")
}

class DesktopAppNode(root: RootProjectNode, c: Constants) : Directory by root.dir("ylcs-app").dir("desktopApp") {
    private val build: Directory = dir("build")

    val originOutput: Directory = build.dir("compose").dir("binaries").dir("main-release").dir("app")
}

class WebAppNode(root: RootProjectNode, c: Constants) : Directory by root.dir("ylcs-app").dir("webApp") {
    private val build: Directory = dir("build")

    val originJsOutput: Directory = build.dir("dist").dir("js").dir("productionExecutable")
    val originWasmOutput: Directory = build.dir("dist").dir("wasmJs").dir("productionExecutable")
    val jsOutput: Directory = root.outputs.dir("webJs")
    val wasmOutput: Directory = root.outputs.dir("webWasm")
}

class LandpageNode(root: RootProjectNode) : Directory by root.dir("ylcs-app").dir("landpage") {
    private val build: Directory = dir("build")

    val originOutput: Directory = build.dir("dist").dir("js").dir("productionExecutable")
    val output: Directory = root.outputs.dir("landpage")
}

class GalleryNode(root: RootProjectNode) : Directory by root.dir("ylcs-app").dir("gallery") {
    private val build: Directory = dir("build")

    val originOutput: Directory = build.dir("dist").dir("wasmJs").dir("productionExecutable")
    val output: Directory = root.docs.gallery
}

class ServerNode(root: RootProjectNode, c: Constants) : Directory by root.dir("ylcs-app").dir("server") {
    val originOutput: RegularFile = dir("build").dir("libs").file(c.server.outputName)
}

class ModManagerNode(root: RootProjectNode, c: Constants) : Directory by root.dir("ylcs-app").dir("mod-manager") {
    private val build: Directory = dir("build")
    val originOutput: Directory = build.dir("compose").dir("binaries").dir("main-release").dir("app")
}

class RootProjectNode(root: Directory, c: Constants) : Directory by root {
    val buildSrc = BuildSrcNode(this)
    val config = ConfigNode(this, c)
    val docs = DocsNode(this)
    val outputs = OutputsNode(this)
    val script = ScriptNode(this)
    val artifacts = ArtifactsNode(this)
    val work = WorkNode(this)
    val androidApp = AndroidAppNode(this, c)
    val iosApp = IosAppNode(this, c)
    val desktopApp = DesktopAppNode(this, c)
    val webApp = WebAppNode(this, c)
    val landpage = LandpageNode(this)
    val gallery = GalleryNode(this)
    val server = ServerNode(this, c)
    val modManager = ModManagerNode(this, c)
    val libsVersion: RegularFile = file("libs.version.toml")
    val license: RegularFile = file("LICENSE")
    val localProperties: RegularFile = file("local.properties")
    val readme: RegularFile = file("README.md")
}