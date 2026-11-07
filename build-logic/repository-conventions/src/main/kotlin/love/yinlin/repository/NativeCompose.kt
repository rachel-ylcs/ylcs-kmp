package love.yinlin.repository

import groovy.json.JsonSlurper
import love.yinlin.nativebridge.NativeBridgeSourcesExtension
import love.yinlin.repository.nativecompose.NativeUpstreamSync
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.getByType
import java.io.File

private fun Settings.includeNativeComposeSources(directory: File) {
    val generation = NativeUpstreamSync(directory).prepare(gradle.startParameter.isOffline)
    val extras = gradle.extensions.extraProperties
    extras.set("love.yinlin.nativeCompose.generation", generation)
    val skiko = JsonSlurper().parse(generation.resolve("metadata/skiko.json")) as Map<*, *>
    for (rule in listOf("skiko-core", "skiko-skottie")) {
        extras.set("love.yinlin.nativeBridge.version.$rule", skiko["version"] as String)
    }
    extensions.getByType<NativeBridgeSourcesExtension>().include(generation.resolve("skiko/workspace/skiko"), "skiko", mapOf(
        "skiko-core" to ("${skiko["group"]}:skiko" to ":"),
        "skiko-skottie" to ("${skiko["group"]}:skiko-skottie" to ":skiko-skottie"),
    ), mapOf(
        "skiko-core" to directory.resolve("adaptation/org.jetbrains.skiko_skiko"),
        "skiko-skottie" to directory.resolve("adaptation/org.jetbrains.skiko_skiko-skottie"),
    ))

    val compose = JsonSlurper().parse(generation.resolve("metadata/compose.json")) as Map<*, *>
    val modules = compose["modules"] as Map<*, *>
    val projects = modules.entries.associate { (artifact, value) ->
        val module = value as Map<*, *>
        val rule = "compose-$artifact"
        gradle.extensions.extraProperties.set("love.yinlin.nativeBridge.version.$rule", module["version"] as String)
        rule to ("${module["group"] ?: compose["group"]}:$artifact" to ":$artifact")
    }
    val adaptations = modules.entries.associate { (artifact, value) ->
        val module = value as Map<*, *>
        "compose-$artifact" to directory.resolve("adaptation/${module["adaptation"] ?: "org.jetbrains.compose.${module["family"] ?: "ui"}_$artifact"}")
    }
    extensions.getByType<NativeBridgeSourcesExtension>().include(generation.resolve("compose/workspace/native-desktop"), "compose-ui", projects, adaptations)
}

@Suppress("unused")
fun Settings.nativeComposeMaintainer() {
    gradle.extensions.extraProperties.set("love.yinlin.nativeCompose.maintainer", true)

    pluginManager.apply("love.yinlin.native-bridge.settings")
    extensions.getByType<RepositorySettings>().modules {
        module("native-compose")
    }

    val directory = settingsDir.resolve("native-compose")
    includeNativeComposeSources(directory)
}