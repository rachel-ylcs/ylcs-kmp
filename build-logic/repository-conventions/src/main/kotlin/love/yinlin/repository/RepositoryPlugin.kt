package love.yinlin.repository

import love.yinlin.gradle.BuildDefaults
import love.yinlin.gradle.BuildPlatform
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

class RepositoryPlugin @Inject constructor(private val objects: ObjectFactory) : Plugin<Settings> {
    override fun apply(target: Settings) {
        val configuration = RepositorySettings()
        val localProperties = LocalProperties.load(target)
        configuration.repositories.aliyun = localProperties.mirrorEnabled
        target.extensions.add("repository", configuration)
        target.extensions.add("localProperties", localProperties)
        target.gradle.settingsEvaluated {
            configureSettings(target, configuration)
            // Freeze plain data before registering the isolated project callback.
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

        val sources = configuration.repositories
        configureRepositories(target.pluginManagement.repositories, sources, plugins = true)
        target.dependencyResolutionManagement {
            configureRepositories(repositories, sources, plugins = false)
            configuration.versionCatalogFile?.let { path ->
                versionCatalogs {
                    create("libs") {
                        from(objects.fileCollection().from(target.layout.settingsDirectory.file(path)))
                    }
                }
            }
        }
    }

    private fun configureRepositories(target: RepositoryHandler, sources: RepositorySettings.Repositories, plugins: Boolean) {
        with(target) {
            if (sources.aliyun) {
                if (sources.mavenCentral) maven("https://maven.aliyun.com/repository/central")
                if (sources.google) maven("https://maven.aliyun.com/repository/google")
                if (plugins && sources.gradlePluginPortal) maven("https://maven.aliyun.com/repository/gradle-plugin")
            }
            if (sources.mavenCentral) mavenCentral()
            if (sources.google) google()
            if (plugins && sources.gradlePluginPortal) gradlePluginPortal()
        }
    }
}

internal class ConfigureRepository(
    private val data: ByteArray,
    private val rootDirectory: String,
    private val modulePaths: List<String>,
    private val localProperties: LocalProperties,
) : IsolatedAction<Project> {
    override fun execute(target: Project) {
        // Each project owns its copy even when project configuration is sequential.
        val configuration = ObjectInputStream(ByteArrayInputStream(data)).use { it.readObject() as RepositorySettings }
        val defaults = configuration.rachel
        val shared = configuration.shared
        val app = configuration.ylcs.app
        val root = File(rootDirectory)
        defaults.namespace = shared.namespace
        defaults.projectVersion = shared.versionCode
        defaults.desktop.packageVersion = shared.versionName
        defaults.desktop.description = app.description
        defaults.desktop.copyright = app.copyright
        defaults.desktop.vendor = app.vendor
        defaults.desktop.licenseFile = root.resolve("LICENSE").absolutePath
        defaults.native.outputDirectory = root.resolve(defaults.native.outputDirectory ?: "artifacts/desktopNative").absolutePath
        defaults.native.jniHeadersDirectory = root.resolve(defaults.native.jniHeadersDirectory ?: "artifacts/include").absolutePath
        defaults.native.resourceTag = when (defaults.platform) {
            BuildPlatform.Windows -> "windows-x64"
            BuildPlatform.Linux -> "linux-x64"
            BuildPlatform.Mac -> "macos-arm64"
        }
        defaults.publishing.apply {
            version = shared.versionName
            homepage = shared.homepage
            developerId = app.name
            developerName = app.displayName
            scmConnection = "scm:git:git${shared.homepage.removePrefix("https")}.git"
        }
        target.extensions.add("rachelDefaults", defaults)
        val directory = target.objects.directoryProperty().fileValue(root).get()
        target.extensions.add("repositoryConfiguration", Constants(target, directory, configuration, defaults))
        target.extensions.add("localProperties", localProperties)
        GeneratedBuildSources.elements(target)
        if (target.path == ":") GeneratedBuildSources.aggregate(target, modulePaths)
    }
}
