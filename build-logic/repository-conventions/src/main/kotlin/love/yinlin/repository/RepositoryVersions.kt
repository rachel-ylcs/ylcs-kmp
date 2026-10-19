package love.yinlin.repository

import java.util.Properties

/** Version data generated from the root catalog when this local plugin is built. */
internal object RepositoryVersions {
    private val values = Properties().apply {
        val resource = RepositoryVersions::class.java.getResourceAsStream("/rachel-repository-versions.properties")
        checkNotNull(resource) { "Missing generated repository version metadata." }.use { load(it) }
    }

    val current: String = values.getProperty("current")
    val code: Int = values.getProperty("code").toInt()
    val minimum: String = values.getProperty("minimum")
    val minimumCode: Int = values.getProperty("minimumCode").toInt()
}
