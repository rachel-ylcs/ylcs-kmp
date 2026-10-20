package love.yinlin.repository

import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.getByType
import java.io.Serializable
import java.util.Properties

@Suppress("unused")
class LocalProperties private constructor(private val values: Map<String, String>, private val loadError: String? = null) : Serializable {
    val mirror: String? get() = this["mirror"]
    val androidKeyName: String? get() = this["androidKeyName"]
    val androidKeyPassword: String? get() = this["androidKeyPassword"]

    operator fun get(key: String): String? {
        check(loadError == null) { loadError ?: "Cannot read local.properties" }
        return values[key]
    }

    companion object {
        @Suppress("UnstableApiUsage")
        internal fun load(settings: Settings): LocalProperties {
            return try {
                val contents = settings.providers.fileContents(settings.layout.settingsDirectory.file("local.properties")).asBytes.get()
                val properties = Properties()
                contents.inputStream().use { properties.load(it) }
                LocalProperties(properties.stringPropertyNames().associateWith { properties.getProperty(it) })
            }
            catch (_: Exception) {
                LocalProperties(emptyMap(), "Cannot read local.properties.")
            }
        }
    }
}

val Settings.localProperties: LocalProperties get() = extensions.getByType<LocalProperties>()
val Project.localProperties: LocalProperties get() = extensions.getByType<LocalProperties>()
