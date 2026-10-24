package love.yinlin.nativebridge

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ComponentSelector
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.konan.target.HostManager
import java.util.Properties

class NativeBridgePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val bridge = project.extensions.create("nativeBridge", NativeBridgeExtension::class.java)
        val compatibility = Properties().apply {
            checkNotNull(NativeBridgePlugin::class.java.classLoader.getResourceAsStream("rachel-native-bridge.properties")).use { load(it) }
        }
        bridge.redirect("skiko-core") {
            upstream.set("org.jetbrains.skiko:skiko")
            upstreamVersion.set(selectedVersion(project, "skiko-core", compatibility.getProperty("skiko.upstream.version")))
            targets.set(setOf("mingw_x64", "linux_x64", "macos_arm64"))
            module.set(compatibility.getProperty("skiko.port.module").substringBeforeLast(':') + ":" + upstreamVersion.get())
        }
        bridge.redirect("skiko-skottie") {
            upstream.set("org.jetbrains.skiko:skiko-skottie")
            upstreamVersion.set(selectedVersion(project, "skiko-skottie", compatibility.getProperty("skiko.upstream.version")))
            targets.set(setOf("mingw_x64", "linux_x64", "macos_arm64"))
            module.set(compatibility.getProperty("skiko.skottie.module").substringBeforeLast(':') + ":" + upstreamVersion.get())
        }
        val application = project.extensions.create("nativeApplication", NativeApplicationExtension::class.java)
        application.baseName.convention(project.name)
        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
            configureRedirects(project, kotlin, bridge)
            project.afterEvaluate {
                bridge.redirects.forEach(NativeDependencyRule::validate)
                if (application.enabled.get()) {
                    SkikoApplicationDependencies.configure(project, kotlin, application, bridge, compatibility)
                    configureApplication(project, kotlin, application)
                }
            }
        }
    }

    private fun configureRedirects(project: Project, kotlin: KotlinMultiplatformExtension, bridge: NativeBridgeExtension) {
        val targetAttribute = Attribute.of("org.jetbrains.kotlin.native.target", String::class.java)
        val metadataTargets = mutableMapOf<String, Set<String>>()
        kotlin.targets.configureEach {
            compilations.configureEach {
                val compilation = this
                if (compilation is KotlinSharedNativeCompilation) {
                    metadataTargets[compilation.compileDependencyConfigurationName] = compilation.konanTargets.map { it.name }.toSet()
                }
            }
        }
        project.configurations.configureEach {
            val configuration = this
            resolutionStrategy.dependencySubstitution {
                val substitutions = this
                substitutions.all {
                    val dependency = this
                    val requested = dependency.requested as? ModuleComponentSelector ?: return@all
                    val target = configuration.attributes.getAttribute(targetAttribute)
                    val platform = configuration.attributes.getAttribute(KotlinPlatformType.attribute)
                    val rules = bridge.redirects.filter { rule ->
                        rule.upstream.get() == "${requested.group}:${requested.module}" && when {
                            target != null -> target in rule.targets.get()
                            metadataTargets[configuration.name] != null ->
                                metadataTargets.getValue(configuration.name).all { it in rule.targets.get() }
                            platform == KotlinPlatformType.common -> {
                                // A common metadata graph may be replaced only if every participating
                                // platform uses this port. Mixed JVM/Android/common graphs stay official.
                                val participants = kotlin.targets.filter { it.platformType != KotlinPlatformType.common }
                                participants.isNotEmpty() && participants.all {
                                    it is KotlinNativeTarget && it.konanTarget.name in rule.targets.get()
                                }
                            }
                            else -> false
                        }
                    }
                    if (rules.isEmpty()) return@all
                    require(rules.size == 1) { "Ambiguous Native redirects for ${requested.displayName} in ${configuration.name}" }
                    val rule = rules.single()
                    rule.validate()
                    NativeVersionSelection.validate(requested, rule.upstreamVersion.get(), rule.name)
                    val localKey = "love.yinlin.nativeBridge.source.${rule.name}"
                    val sourceSelector = project.gradle.extensions.extraProperties.let {
                        if (it.has(localKey)) it.get(localKey) as ComponentSelector else null
                    }
                    val maintainer = project.gradle.extensions.extraProperties.let {
                        it.has("love.yinlin.nativeCompose.maintainer") && it.get("love.yinlin.nativeCompose.maintainer") == true
                    }
                    check(!maintainer || sourceSelector != null || rule.projectPath.isPresent) {
                        "Native Compose maintainer mode requires a local project for ${rule.name}; Maven fallback is disabled. Register its source project in nativeBridgeSources."
                    }
                    val replacement = sourceSelector ?: if (rule.projectPath.isPresent) substitutions.project(rule.projectPath.get())
                    else substitutions.module(rule.module.get())
                    dependency.useTarget(replacement, "Rachel Native port: ${rule.name}, target=${target ?: "common"}")
                }
            }
        }
    }

    private fun selectedVersion(project: Project, rule: String, fallback: String): String {
        val properties = project.gradle.extensions.extraProperties
        val key = "love.yinlin.nativeBridge.version.$rule"
        return if (properties.has("love.yinlin.nativeCompose.maintainer") && properties.get("love.yinlin.nativeCompose.maintainer") == true && properties.has(key)) properties.get(key) as String
        else fallback
    }

    private fun configureApplication(project: Project, kotlin: KotlinMultiplatformExtension, application: NativeApplicationExtension) {
        kotlin.targets.withType(KotlinNativeTarget::class.java).configureEach {
            val target = this
            require(target.konanTarget.name in setOf("mingw_x64", "linux_x64", "macos_arm64")) {
                "Native application packaging supports mingw_x64, linux_x64 and macos_arm64; got ${target.konanTarget.name}"
            }
            if (target.binaries.withType(Executable::class.java).isEmpty()) {
                target.binaries.executable {
                    entryPoint = application.entryPoint.get()
                    baseName = application.baseName.get()
                }
            }
            val runtime = application.runtimes.maybeCreate(target.konanTarget.name)
            val renamedArtifacts = runtime.artifacts.get().map { (coordinate, filename) ->
                require(filename.isNotBlank() && filename !in setOf(".", "..") && filename.none { it in "/\\:" }) {
                    "Runtime artifact filename must be a simple filename: $filename"
                }
                val configuration = project.configurations.detachedConfiguration(project.dependencies.create(coordinate))
                configuration.isTransitive = false
                configuration to filename
            }
            val runtimeConfiguration = project.configurations.create("${target.name}NativeApplicationRuntime") {
                isCanBeConsumed = false
                isCanBeResolved = true
                isTransitive = false
                defaultDependencies {
                    runtime.dependencies.get().forEach { coordinate -> add(project.dependencies.create(coordinate)) }
                }
            }
            target.binaries.withType(Executable::class.java).configureEach {
                val binary = this
                val suffix = target.name.replaceFirstChar(Char::uppercase) + binary.name.replaceFirstChar(Char::uppercase)
                val stage = project.tasks.register("stage$suffix", Sync::class.java) {
                    group = "native application"
                    description = "Stage ${target.konanTarget.name} ${binary.name} with its runtime files"
                    dependsOn(binary.linkTaskProvider)
                    from(project.provider { binary.outputFile }) {
                        filePermissions { unix("rwxr-xr-x") }
                    }
                    from(runtimeConfiguration)
                    from(runtime.files)
                    renamedArtifacts.forEach { (configuration, filename) ->
                        from(configuration) { rename { filename } }
                    }
                    from(application.resources)
                    duplicatesStrategy = DuplicatesStrategy.FAIL
                    into(project.layout.buildDirectory.dir("nativeApplication/${target.name}/${binary.name}"))
                    doFirst {
                        require(HostManager.host == target.konanTarget) { "Stage and verify this executable on ${target.konanTarget.name}" }
                        val missing = runtime.files.files.filterNot { file -> file.exists() }
                        require(missing.isEmpty()) { "Missing native runtime files: ${missing.joinToString()}" }
                    }
                }
                project.tasks.register("run$suffix", RunNativeApplication::class.java) {
                    group = "native application"
                    dependsOn(stage)
                    distribution.set(project.layout.dir(stage.map { task -> task.destinationDir }))
                    executableName.set(project.provider { binary.outputFile.name })
                    arguments.set(application.arguments)
                }
                project.tasks.register("package$suffix", Zip::class.java) {
                    group = "native application"
                    dependsOn(stage)
                    from(stage.map { task -> task.destinationDir })
                    archiveBaseName.set(application.baseName)
                    archiveClassifier.set("${target.konanTarget.name}-${binary.buildType.name.lowercase()}")
                    destinationDirectory.set(project.layout.buildDirectory.dir("distributions"))
                    isPreserveFileTimestamps = false
                    isReproducibleFileOrder = true
                }
            }
        }
    }
}
