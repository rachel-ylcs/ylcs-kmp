package love.yinlin.repository

import love.yinlin.gradle.BuildDefaults
import java.io.Serializable

/** Local-only configuration. The public plugin has no dependency on this model. */
class RepositorySettings : Serializable {
    var name: String? = null
    var typeSafeProjectAccessors: Boolean = false
    var versionCatalogFile: String? = "libs.versions.toml"
    internal val modulePaths: MutableList<String> = mutableListOf()
    val repositories = Repositories()
    val shared = Shared()
    val rachel = BuildDefaults()
    val ylcs = Ylcs()

    /** Declares project paths to register after the settings DSL has finished. */
    fun modules(vararg paths: String) { modulePaths.addAll(paths) }
    fun modules(block: RepositoryModules.() -> Unit) { RepositoryModules(modulePaths).block() }
    fun repositories(block: Repositories.() -> Unit) { repositories.block() }
    fun shared(block: Shared.() -> Unit) { shared.block() }
    fun rachel(block: BuildDefaults.() -> Unit) { rachel.block() }
    fun ylcs(block: Ylcs.() -> Unit) { ylcs.block() }

    class Repositories : Serializable {
        var mavenCentral: Boolean = true
        var google: Boolean = true
        var gradlePluginPortal: Boolean = true
        var aliyun: Boolean = false
    }

    class Shared : Serializable {
        var namespace: String = ""
        val versionCode: Int = RepositoryVersions.code
        val versionName: String = RepositoryVersions.current
        var homepage: String = ""
    }

    class Ylcs : Serializable {
        val app = Application()
        val host = Host()
        val modManager = ModManager()
        var desktopModules: List<String> = emptyList()

        fun app(block: Application.() -> Unit) { app.block() }
        fun host(block: Host.() -> Unit) { host.block() }
        fun modManager(block: ModManager.() -> Unit) { modManager.block() }
    }

    class Application : Serializable {
        var name: String = ""
        var projectName: String = ""
        var displayName: String = ""
        val minVersion: Int = RepositoryVersions.minimumCode
        val minVersionName: String = RepositoryVersions.minimum
        var description: String = ""
        var vendor: String = ""
        var copyright: String = ""
    }

    class Host : Serializable {
        var environment: BuildEnvironment = BuildEnvironment.Prod
        var mainHost: String = ""
        var mainPort: Int = 0
        var webServerPort: Int = 0
    }

    class ModManager : Serializable {
        var name: String = ""
        var displayName: String = ""
        var mainClass: String = ""
    }
}
