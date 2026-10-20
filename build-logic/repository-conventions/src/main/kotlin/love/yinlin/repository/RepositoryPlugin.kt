package love.yinlin.repository

import love.yinlin.gradle.GeneratedBuildSources
import org.gradle.api.IsolatedAction
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.maven
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import javax.inject.Inject

@Suppress("unused", "UnstableApiUsage")
class RepositoryPlugin @Inject constructor(private val objects: ObjectFactory) : Plugin<Settings> {
    override fun apply(target: Settings) {
        val configuration = RepositorySettings()
        val localProperties = LocalProperties.load(target)
        target.extensions.add("repository", configuration)
        target.extensions.add("localProperties", localProperties)
        target.gradle.settingsEvaluated {
            configureSettings(target, configuration)
            val bytes = ByteArrayOutputStream().use { buffer ->
                ObjectOutputStream(buffer).use { it.writeObject(configuration) }
                buffer.toByteArray()
            }
            val descriptors = ArrayDeque(target.rootProject.children)
            val modulePaths = mutableListOf<String>()
            while (descriptors.isNotEmpty()) {
                val descriptor = descriptors.removeFirst()
                modulePaths.add(descriptor.path)
                descriptors.addAll(descriptor.children)
            }
            target.gradle.lifecycle.beforeProject(ConfigureRepository(bytes, target.settingsDir.absolutePath, modulePaths, localProperties))
        }
    }

    private fun configureSettings(target: Settings, configuration: RepositorySettings) {
        configuration.name?.let { target.rootProject.name = it }
        if (configuration.typeSafeProjectAccessors) target.enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
        target.include(*configuration.modulePaths.toTypedArray())

        val properties = target.localProperties
        configureRepositories(target.pluginManagement.repositories, properties, plugins = true)
        target.dependencyResolutionManagement {
            configureRepositories(repositories, properties, plugins = false)
            versionCatalogs {
                create("libs") {
                    from(objects.fileCollection().from(target.layout.settingsDirectory.file("libs.versions.toml")))
                }
            }
        }
    }

    private fun configureRepositories(target: RepositoryHandler, properties: LocalProperties, plugins: Boolean) {
        with(target) {
            val mirror = properties.mirror?.removeSuffix("/")
            if (mirror != null) {
                maven("$mirror/central")
                maven("$mirror/google")
                if (plugins) maven("$mirror/gradle-plugin")
            }
            mavenCentral()
            google()
            if (plugins) gradlePluginPortal()
        }
    }
}

@Suppress("UnstableApiUsage")
internal class ConfigureRepository(
    private val data: ByteArray,
    private val rootDirectory: String,
    private val modulePaths: List<String>,
    private val localProperties: LocalProperties,
) : IsolatedAction<Project> {
    override fun execute(target: Project) {
        val configuration = ObjectInputStream(ByteArrayInputStream(data)).use { it.readObject() as RepositorySettings }
        val defaults = configuration.rachel
        val app = configuration.ylcs.app
        val root = File(rootDirectory)
        defaults.namespace = configuration.namespace
        defaults.organization = configuration.organization
        defaults.copyright = configuration.copyright
        defaults.versionCode = configuration.versionCode
        defaults.desktop.packageVersion = configuration.versionName
        defaults.desktop.licenseFile = root.resolve("LICENSE").absolutePath
        defaults.native.outputDirectory = root.resolve(defaults.native.outputDirectory ?: "artifacts/desktopNative").absolutePath
        defaults.native.jniHeadersDirectory = root.resolve(defaults.native.jniHeadersDirectory ?: "artifacts/include").absolutePath
        defaults.publishing.apply {
            version = configuration.versionName
            homepage = configuration.homepage
            developerId = app.name
            developerName = app.displayName
            scmConnection = "scm:git:git${configuration.homepage.removePrefix("https")}.git"
        }
        target.extensions.add("rachelDefaults", defaults)
        val directory = target.objects.directoryProperty().fileValue(root).get()
        target.extensions.add("repositoryConfiguration", Constants(target, directory, configuration, defaults))
        target.extensions.add("localProperties", localProperties)
        GeneratedBuildSources.elements(target)
        if (target.path == ":") GeneratedBuildSources.aggregate(target, modulePaths)
    }
}
