package love.yinlin.project

import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile

class BuildSrcNode(root: RootProjectNode) : Directory by root.dir("buildSrc")

class AppNode(val root: RootProjectNode, c: Constants) : Directory by root.dir("ylcs-app") {
    class ConfigNode(parent: AppNode, c: Constants) : Directory by parent.dir("config") {
        val icon: RegularFile = file(when (c.platform) {
            BuildPlatform.Windows -> "icon.ico"
            BuildPlatform.Linux -> "icon.png"
            BuildPlatform.Mac -> "icon.icns"
        })
        val secret: Directory = dir("secret")
        val androidKey: RegularFile = secret.file("androidKey.jks")
    }
    val config = ConfigNode(this, c)

    class ScriptNode(parent: AppNode) : Directory by parent.dir("script")

    class AndroidAppNode(parent: AppNode) : Directory by parent.dir("android-app")

    class IosAppNode(parent: AppNode) : Directory by parent.dir("iosApp") {
        val podfile: RegularFile = file("Podfile")
        val configuration: Directory = dir("Configuration")
        val configurationFile: RegularFile = configuration.file("Version.xcconfig")
    }
    val iosApp = IosAppNode(this)

    class DesktopAppNode(parent: AppNode) : Directory by parent.dir("desktop-app") {
        private val build: Directory = dir("build")

        val originOutput: Directory = build.dir("compose").dir("binaries").dir("main-release").dir("app")
    }
    val desktopApp = DesktopAppNode(this)

    class WebAppNode(parent: AppNode) : Directory by parent.dir("web-app") {
        private val build: Directory = dir("build")

        val originJsOutput: Directory = build.dir("dist").dir("js").dir("productionExecutable")
        val originWasmOutput: Directory = build.dir("dist").dir("wasmJs").dir("productionExecutable")
        val jsOutput: Directory = parent.root.outputs.dir("webJs")
        val wasmOutput: Directory = parent.root.outputs.dir("webWasm")
    }
    val webApp = WebAppNode(this)

    class LandpageNode(parent: AppNode) : Directory by parent.dir("landpage") {
        private val build: Directory = dir("build")

        val originOutput: Directory = build.dir("dist").dir("js").dir("productionExecutable")
        val output: Directory = parent.root.outputs.dir("landpage")
    }
    val landpage = LandpageNode(this)

    class GalleryNode(parent: AppNode) : Directory by parent.dir("gallery") {
        private val build: Directory = dir("build")

        val originOutput: Directory = build.dir("dist").dir("wasmJs").dir("productionExecutable")
        val output: Directory = parent.root.docs.gallery
    }
    val gallery = GalleryNode(this)

    class ServerNode(parent: AppNode, c: Constants) : Directory by parent.dir("server") {
        val originOutput: RegularFile = dir("build").dir("libs").file(c.server.outputName)
    }
    val server = ServerNode(this, c)

    class ModManagerNode(parent: AppNode) : Directory by parent.dir("mod-manager") {
        private val build: Directory = dir("build")
        val originOutput: Directory = build.dir("compose").dir("binaries").dir("main-release").dir("app")
    }
    val modManager = ModManagerNode(this)
}

class DocsNode(root: RootProjectNode) : Directory by root.dir("ylcs-docs") {
    val mkdocs: RegularFile = file("mkdocs.yml")
    val docs: Directory = dir("docs")
    val overrides: Directory = docs.dir("overrides")
    val dokka: Directory = overrides.dir("dokka")
    val gallery: Directory = overrides.dir("gallery")
}

class OutputsNode(root: RootProjectNode) : Directory by root.dir("outputs")

class ArtifactsNode(root: RootProjectNode) : Directory by root.dir("artifacts") {
    val include: Directory = dir("include")
    val desktopNative: Directory = dir("desktopNative")
}

class WorkNode(root: RootProjectNode) : Directory by root.dir("work") {
    val desktop: Directory = dir("desktop")
    val server: Directory = dir("server")
    val modManager: Directory = dir("modManager")
}

class RootProjectNode(root: Directory, c: Constants) : Directory by root {
    val libsVersion: RegularFile = file("libs.version.toml")
    val license: RegularFile = file("LICENSE")
    val localProperties: RegularFile = file("local.properties")
    val readme: RegularFile = file("README.md")
    val docs = DocsNode(this)
    val artifacts = ArtifactsNode(this)
    val work = WorkNode(this)
    val outputs = OutputsNode(this)
    val buildSrc = BuildSrcNode(this)
    val app = AppNode(this, c)
}