package love.yinlin.repository

import love.yinlin.gradle.BuildPlatform
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile

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

    class IosAppNode(parent: AppNode) : Directory by parent.dir("iosApp") {
        val configurationFile: RegularFile = dir("Configuration").file("Version.xcconfig")
    }
    val iosApp = IosAppNode(this)
}

class DocsNode(root: RootProjectNode) : Directory by root.dir("ylcs-docs") {
    val mkdocs: RegularFile = file("mkdocs.yml")
    val docs: Directory = dir("docs")
    val overrides: Directory = docs.dir("overrides")
    val dokka: Directory = overrides.dir("dokka")
    val gallery: Directory = overrides.dir("gallery")
    val modernScreen: Directory = overrides.dir("modern-screen")
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
    val libsVersion: RegularFile = file("libs.versions.toml")
    val license: RegularFile = file("LICENSE")
    val localProperties: RegularFile = file("local.properties")
    val readme: RegularFile = file("README.md")
    val docs = DocsNode(this)
    val artifacts = ArtifactsNode(this)
    val work = WorkNode(this)
    val outputs = OutputsNode(this)
    val app = AppNode(this, c)
}
