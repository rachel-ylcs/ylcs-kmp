package love.yinlin.repository

import love.yinlin.gradle.BuildDefaults
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.kotlin.dsl.getByType

/** Compatibility read facade for local build scripts; never used by the public plugin. */
class Constants(project: Project, directory: Directory, configuration: RepositorySettings, defaults: BuildDefaults) {
    val environment = configuration.ylcs.host.environment
    val platform = defaults.platform
    val architecture = defaults.architecture
    val platformVersion: String = System.getProperty("os.version") ?: ""
    val namespace = configuration.shared.namespace
    val resourceTag = defaults.native.resourceTag!!
    val gradleVersion: String = project.gradle.gradleVersion
    val javaVersion: String = System.getProperty("java.version") ?: ""
    val maxMemory: Long = Runtime.getRuntime().maxMemory()
    val totalMemory: Long = Runtime.getRuntime().totalMemory()
    val freeMemory: Long = Runtime.getRuntime().freeMemory()
    val app = Application(configuration.shared, configuration.ylcs.app)
    val jvm = defaults.jvm
    val android = defaults.android
    val ios = defaults.ios
    val desktop = Desktop(configuration.ylcs.desktopModules)
    val modManager = configuration.ylcs.modManager
    val root = RootProjectNode(directory, this)
    val host = Host(configuration.ylcs.host)

    class Application(shared: RepositorySettings.Shared, app: RepositorySettings.Application) {
        val name = app.name
        val projectName = app.projectName
        val displayName = app.displayName
        val version = shared.versionCode
        val versionName = shared.versionName
        val minVersion = app.minVersion
        val minVersionName = app.minVersionName
        val description = app.description
        val vendor = app.vendor
        val copyright = app.copyright
        val homepage = shared.homepage
        val packageName = shared.namespace
        val mainClass = "$packageName.MainKt"
    }

    class Host(host: RepositorySettings.Host) {
        val mainHost = host.mainHost
        val mainPort = host.mainPort
        val webServerPort = host.webServerPort
        val apiHost = "api.$mainHost"
        val apiUrl = when (host.environment) {
            BuildEnvironment.Dev -> "http://localhost:$mainPort"
            BuildEnvironment.Prod -> "https://$apiHost"
        }
    }

    class Desktop(val modules: List<String>)
}

val Project.C: Constants get() = extensions.getByType<Constants>()

// Diagnostic compatibility only; execution behavior must be configured on the relevant tasks.
val Project.currentTaskName: String get() = gradle.startParameter.taskNames.firstOrNull() ?: "sync"
