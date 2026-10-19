package love.yinlin.repository

import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.getByType
import java.io.Serializable
import java.util.Properties

/** Read-only local configuration, loaded once by the repository settings plugin for each configuration run. */
class LocalProperties private constructor(
    private val values: Map<String, String>,
    private val loadError: String? = null,
) : Serializable {
    // Preserve the existing presence-based flag, including mirror.enabled=false.
    val mirrorEnabled: Boolean get() = loadError == null && "mirror.enabled" in values
    val androidKeyName: String get() = requireNotNull(this["androidKeyName"]) { "Missing androidKeyName in local.properties" }
    val androidKeyPassword: String? get() = this["androidKeyPassword"]

    /** Returns null for an absent key; an unreadable or malformed file fails at the point of use. */
    operator fun get(key: String): String? {
        check(loadError == null) { loadError ?: "Cannot read local.properties" }
        return values[key]
    }

    companion object {
        internal fun load(settings: Settings): LocalProperties {
            return try {
                // Gradle tracks the contents (including absence) as a configuration-cache input.
                val contents = settings.providers.fileContents(settings.layout.settingsDirectory.file("local.properties")).asBytes.orNull
                    ?: return LocalProperties(emptyMap(), "local.properties does not exist")
                val properties = Properties().apply {
                    contents.inputStream().use { load(it) }
                }
                LocalProperties(properties.stringPropertyNames().associateWith { properties.getProperty(it) })
            }
            catch (e: Exception) {
                // Keep failure handling at each caller, without retaining or logging file contents.
                LocalProperties(emptyMap(), "Cannot read local.properties (${e.javaClass.simpleName})")
            }
        }
    }
}

val Settings.localProperties: LocalProperties get() = extensions.getByType<LocalProperties>()

/** Use during configuration; pass individual values to task inputs for execution. */
val Project.localProperties: LocalProperties get() = extensions.getByType<LocalProperties>()
