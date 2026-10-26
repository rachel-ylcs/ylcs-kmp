package love.yinlin.nativebridge

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ComponentSelector
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.invocation.Gradle
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
        for (artifact in compatibility.getProperty("compose.ui.modules").split(',')) {
            val ruleName = "compose-$artifact"
            bridge.redirect(ruleName) {
                upstream.set(compatibility.getProperty("compose.$artifact.upstream", "org.jetbrains.compose.ui:$artifact"))
                upstreamVersion.set(selectedVersion(project, ruleName, compatibility.getProperty("compose.$artifact.upstream.version", compatibility.getProperty("compose.upstream.version"))))
                targets.set(compatibility.getProperty("compose.$artifact.targets").split(',').toSet())
                metadataTargets.set(compatibility.getProperty("compose.$artifact.metadata.targets").split(',').toSet())
                module.set(compatibility.getProperty("compose.$artifact.module").substringBeforeLast(':') + ":" + upstreamVersion.get())
            }
        }
        val application = project.extensions.create("nativeApplication", NativeApplicationExtension::class.java)
        application.baseName.convention(project.name)
        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
            configureReport(project, kotlin, bridge)
            NativePortDependencyAudit.configure(project, kotlin, bridge) { sourceProperty(project, "source", it) as? ComponentSelector }
            configureRedirects(project, kotlin, bridge)
            project.afterEvaluate {
                bridge.redirects.forEach(NativeDependencyRule::validate)
                val ownedRules = compatibility.getProperty("compose.ui.modules").split(',').map { "compose-$it" } + listOf("skiko-core", "skiko-skottie")
                for (name in ownedRules) {
                    val rule = bridge.redirects.getByName(name)
                    require("macos_arm64" in rule.targets.get() && "macos_arm64" in rule.metadataTargets.get()) { "$name: macOS is an owned Native target; official artifact fallback is disabled" }
                    if (rule.module.isPresent) require(rule.module.get().substringBefore(':') != rule.upstream.get().substringBefore(':')) { "$name: an owned port cannot redirect back to the official Maven group" }
                }
                NativeMetadataAliases.configure(project, kotlin, bridge)
                if (project.plugins.hasPlugin("org.jetbrains.compose")) ComposeResourceGeneration.configure(project, kotlin, bridge)
                if (bridge.skikoTestRuntimeEnabled.get()) SkikoTestRuntime.configure(project, bridge)
                if (application.enabled.get()) {
                    SkikoApplicationDependencies.configure(project, application, bridge)
                    configureApplication(project, kotlin, application)
                }
                ComposeApplicationResources.configure(project, kotlin, application.enabled.get())
            }
        }
    }

    private fun configureReport(project: Project, kotlin: KotlinMultiplatformExtension, bridge: NativeBridgeExtension) {
        project.tasks.register("nativeBridgeReport") {
            group = "help"
            description = "Show configured Native redirects, local adaptation scripts and resolution configurations"
            doLast {
                val nativeTargets = kotlin.targets.withType(KotlinNativeTarget::class.java)
                project.logger.lifecycle("Native Bridge configured redirects for ${project.path} (no dependency resolution)")
                for (rule in bridge.redirects.filter { it.metadataTargets.get() != it.targets.get() }) {
                    project.logger.lifecycle("Shared metadata ${rule.name}: ${rule.metadataTargets.get().joinToString()}; concrete redirects: ${rule.targets.get().joinToString()}")
                }
                if (nativeTargets.isEmpty()) project.logger.lifecycle("No Native targets; no Native redirects apply.")
                for (target in nativeTargets) {
                    val configuration = target.compilations.getByName("main").compileDependencyConfigurationName
                    project.logger.lifecycle("Target ${target.name} (${target.konanTarget.name}); configuration=$configuration")
                    for (rule in bridge.redirects.filter { target.konanTarget.name in it.targets.get() }) {
                        val source = sourceProperty(project, "sourceInfo", rule.name) as? Map<*, *>
                        val replacement = when {
                            source != null -> "included build ${source["build"]}, project ${source["project"]}"
                            rule.projectPath.isPresent -> "project ${rule.projectPath.get()}"
                            else -> "Maven ${rule.module.get()}"
                        }
                        project.logger.lifecycle("  ${rule.name}: ${rule.upstream.get()}:${rule.upstreamVersion.get()} -> $replacement")
                        val adaptation = source?.get("adaptation") as? String
                        if (!adaptation.isNullOrBlank()) project.logger.lifecycle("    adaptation: $adaptation")
                    }
                    val insight = if (project.path == ":") ":dependencyInsight" else "${project.path}:dependencyInsight"
                    project.logger.lifecycle("  Actual graph: $insight --configuration $configuration --dependency <group:artifact>")
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
                        rule.matchesUpstream(requested.group, requested.module, target) && when {
                            target != null -> target in rule.targets.get()
                            metadataTargets[configuration.name] != null ->
                                metadataTargets.getValue(configuration.name).all { it in rule.metadataTargets.get() }
                            platform == KotlinPlatformType.common -> {
                                // Shared metadata may be replaced only when the replacement covers
                                // every participant. Mixed JVM/Android/common graphs stay official.
                                val participants = kotlin.targets.filter { it.platformType != KotlinPlatformType.common }
                                participants.isNotEmpty() && participants.all {
                                    it is KotlinNativeTarget && it.konanTarget.name in rule.metadataTargets.get()
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
                    val sourceSelector = sourceProperty(project, "source", rule.name) as? ComponentSelector
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

    private fun sourceProperty(project: Project, property: String, rule: String): Any? {
        val key = "love.yinlin.nativeBridge.$property.$rule"
        var build: Gradle? = project.gradle
        // Gradle flattens duplicate included builds and may register their selectors in the parent only.
        while (build != null) {
            val extras = build.extensions.extraProperties
            if (extras.has(key)) return extras.get(key)
            build = build.parent
        }
        return null
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
            if (target.konanTarget.name !in setOf("mingw_x64", "linux_x64", "macos_arm64")) return@configureEach
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
                NativeRuntimeAttributes.configure(project, configuration, target.konanTarget.name)
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
            NativeRuntimeAttributes.configure(project, runtimeConfiguration, target.konanTarget.name)
            target.binaries.withType(Executable::class.java).configureEach {
                val binary = this
                if (target.konanTarget.name == "mingw_x64" && (application.desktop.enabled || application.skikoWindowEnabled.get())) {
                    // Keep Kotlin's main/CRT initialization while asking Windows to launch a GUI process.
                    binary.linkerOpts("-Wl,--subsystem,windows", "-Wl,--entry,mainCRTStartup")
                }
                val suffix = target.name.replaceFirstChar(Char::uppercase) + binary.name.replaceFirstChar(Char::uppercase)
                val desktop = application.desktop
                if (desktop.enabled) desktop.validate()
                val executableDirectory = NativeDesktopPackaging.executableDirectory(desktop, target.konanTarget.name)
                val resourceDirectory = NativeDesktopPackaging.resourceDirectory(desktop, target.konanTarget.name)
                val executablePath = listOf(executableDirectory, binary.outputFile.name).filter { it.isNotEmpty() }.joinToString("/")
                val composeResources = ComposeApplicationResources.register(project, target, binary, suffix)
                val stage = project.tasks.register("stage$suffix", Sync::class.java) {
                    group = "native application"
                    description = "Stage ${target.konanTarget.name} ${binary.name} with its runtime files"
                    dependsOn(binary.linkTaskProvider)
                    from(project.provider { binary.outputFile }) {
                        into(executableDirectory)
                        filePermissions { unix("rwxr-xr-x") }
                    }
                    from(runtimeConfiguration) { into(executableDirectory) }
                    from(runtime.files) { into(executableDirectory) }
                    renamedArtifacts.forEach { (configuration, filename) ->
                        from(configuration) {
                            into(executableDirectory)
                            rename { filename }
                        }
                    }
                    from(application.resources) { into(resourceDirectory) }
                    if (composeResources != null) from(composeResources) { into(listOf(resourceDirectory, "composeResources").filter { it.isNotEmpty() }.joinToString("/")) }
                    inputs.property("desktop.enabled", desktop.enabled)
                    if (desktop.enabled) {
                        inputs.properties(desktop.metadata().mapKeys { "desktop.${it.key}" })
                        inputs.files(listOfNotNull(desktop.icon(target.konanTarget.name))).withPropertyName("desktopIcon")
                    }
                    duplicatesStrategy = DuplicatesStrategy.FAIL
                    into(project.layout.buildDirectory.dir("nativeApplication/${target.name}/${binary.name}"))
                    doFirst {
                        require(HostManager.host == target.konanTarget) { "Stage and verify this executable on ${target.konanTarget.name}" }
                        val missing = runtime.files.files.filterNot { file -> file.exists() }
                        require(missing.isEmpty()) { "Missing native runtime files: ${missing.joinToString()}" }
                    }
                    doLast { NativeDesktopPackaging.finish(destinationDir, destinationDir.resolve(executablePath), target.konanTarget.name, desktop) }
                }
                project.tasks.register("run$suffix", RunNativeApplication::class.java) {
                    group = "native application"
                    dependsOn(stage)
                    distribution.set(project.layout.dir(stage.map { task -> task.destinationDir }))
                    executableName.set(executablePath)
                    arguments.set(application.arguments)
                }
                project.tasks.register("package$suffix", Zip::class.java) {
                    group = "native application"
                    dependsOn(stage)
                    from(stage.map { task -> task.destinationDir }) {
                        include { it.relativePath.pathString == executablePath }
                        filePermissions { unix("rwxr-xr-x") }
                    }
                    from(stage.map { task -> task.destinationDir }) {
                        exclude { it.relativePath.pathString == executablePath }
                        filePermissions { unix("rw-r--r--") }
                    }
                    dirPermissions { unix("rwxr-xr-x") }
                    archiveBaseName.set(application.baseName)
                    archiveClassifier.set("${target.konanTarget.name}-${binary.buildType.name.lowercase()}")
                    destinationDirectory.set(project.layout.buildDirectory.dir("distributions"))
                    isPreserveFileTimestamps = false
                    isReproducibleFileOrder = true
                }
            }
        }
        NativeComposeTasks.configure(project, kotlin)
    }
}
