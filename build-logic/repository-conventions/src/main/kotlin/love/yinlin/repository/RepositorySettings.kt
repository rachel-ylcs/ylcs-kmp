package love.yinlin.repository

import love.yinlin.gradle.BuildDefaults
import java.io.Serializable

@Suppress("unused")
class RepositorySettings : Serializable {
    /**
     * 仓库名称
     */
    var name: String? = null

    /**
     * 使用安全项目访问器
     */
    var typeSafeProjectAccessors: Boolean = true

    /**
     * 命名空间
     */
    var namespace: String = ""

    /**
     * 主页
     */
    var homepage: String = ""

    /**
     * 版本号
     */
    val versionName: String = RepositoryVersions.current

    /**
     * 版本号值
     */
    val versionCode: Int = RepositoryVersions.code

    /**
     * 组织
     */
    var organization: String = ""

    /**
     * 版权声明
     */
    var copyright: String = ""

    internal val modulePaths: MutableList<String> = mutableListOf()

    fun modules(block: RepositoryModules.() -> Unit) { RepositoryModules(modulePaths).block() }

    internal val rachel = BuildDefaults()

    fun rachel(block: BuildDefaults.() -> Unit) { rachel.block() }

    internal val ylcs = Ylcs()

    fun ylcs(block: Ylcs.() -> Unit) { ylcs.block() }

    class Ylcs : Serializable {
        val app = Application()
        val cs = CS()

        fun app(block: Application.() -> Unit) { app.block() }
        fun cs(block: CS.() -> Unit) { cs.block() }
    }

    class Application : Serializable {
        /**
         * APP 英文名
         */
        var name: String = ""

        /**
         * APP 项目名
         */
        var projectName: String = ""

        /**
         * APP 中文名
         */
        var displayName: String = ""

        /**
         * APP 最低兼容版本号
         */
        val minVersion: Int = RepositoryVersions.minimumCode

        /**
         * APP 最大兼容版本
         */
        val minVersionName: String = RepositoryVersions.minimum
    }

    class CS : Serializable {
        /**
         * API地址
         */
        var apiUrl: String = ""
    }
}
