package love.yinlin.repository

import love.yinlin.gradle.BuildDefaults
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.kotlin.dsl.getByType

@Suppress("unused")
class Constants(
    project: Project,
    directory: Directory,
    configuration: RepositorySettings,
    defaults: BuildDefaults
) {
    val version = configuration.versionCode
    val versionName = configuration.versionName
    val platform = defaults.platform
    val architecture = defaults.architecture
    val namespace = defaults.namespace

    val app = Application(configuration.ylcs.app)
    val cs = CS(configuration.ylcs.cs)

    val root = RootProjectNode(directory, this)

    val jvm = defaults.jvm
    val android = defaults.android
    val ios = defaults.ios
    val desktop = defaults.desktop
    val web = defaults.web
    val native = defaults.native

    class Application(app: RepositorySettings.Application) {
        val name = app.name
        val projectName = app.projectName
        val displayName = app.displayName
        val minVersion = app.minVersion
        val minVersionName = app.minVersionName
    }

    class CS(cs: RepositorySettings.CS) {
        val apiUrl = cs.apiUrl
    }
}

val Project.C: Constants get() = extensions.getByType<Constants>()