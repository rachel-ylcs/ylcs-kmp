package love.yinlin.repository

import groovy.json.JsonSlurper
import love.yinlin.nativebridge.NativeBridgeSourcesExtension
import love.yinlin.repository.nativecompose.NativeUpstreamSync
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.getByType
import java.io.File
import java.security.MessageDigest

/**
 * 本仓库始终重现锁定的移植源码并关联真实 project。
 * 仓库外消费者只应用 Project 桥插件，按 target 选择已发布 Maven 工件。
 */
@Suppress("UnstableApiUsage")
fun Settings.nativeCompose() {
    gradle.extensions.extraProperties.set("love.yinlin.nativeCompose.maintainer", true)

    pluginManager.apply("love.yinlin.native-bridge.settings")
    extensions.getByType<RepositorySettings>().modules {
        module("native-compose")
    }

    val directory = settingsDir.resolve("native-compose")
    includeNativeComposeSources(directory)
}

private fun Settings.includeNativeComposeSources(directory: File) {
    NativeUpstreamSync(directory).prepare(gradle.startParameter.isOffline)
    val stateFile = directory.resolve("vendor/skiko/state.json")
    check(stateFile.isFile) { "Source preparation did not produce vendor/skiko/state.json" }

    val state = JsonSlurper().parse(stateFile) as Map<*, *>
    val repository = settingsDir.canonicalFile.toPath()
    val inputs = state["inputs"] as Map<*, *>
    for ((name, expected) in inputs) {
        val input = settingsDir.resolve(name as String).canonicalFile
        require(input.toPath().startsWith(repository)) { "Invalid prepared input path: $name" }
        check(input.isFile && sha256(input) == expected) { "Prepared input changed: $name; re-run prepareNativeSources" }
    }
    check(sha256(settingsDir.resolve("libs.versions.toml")) == state["catalogSha256"]) { "Prepared build does not use the root version catalog" }
    val versions = state["versions"] as Map<*, *>
    gradle.extensions.extraProperties.set("love.yinlin.nativeCompose.root", directory)
    for (rule in listOf("skiko-core", "skiko-skottie")) {
        gradle.extensions.extraProperties.set("love.yinlin.nativeBridge.version.$rule", versions["skiko"] as String)
    }

    val sourceDirectory = directory.resolve(state["build"] as String).canonicalFile
    require(sourceDirectory == directory.resolve("vendor/skiko/workspace/skiko").canonicalFile) { "Invalid prepared source directory" }
    check(sourceDirectory.isDirectory) { "Prepared Native Compose working tree is missing" }
    val lock = JsonSlurper().parse(directory.resolve("upstream/lock.json")) as Map<*, *>
    extensions.getByType<NativeBridgeSourcesExtension>().include(sourceDirectory, "skiko", mapOf(
        "skiko-core" to ("${lock["group"]}:skiko" to ":"),
        "skiko-skottie" to ("${lock["group"]}:skiko-skottie" to ":skiko-skottie"),
    ), mapOf(
        "skiko-core" to directory.resolve("adaptation/org.jetbrains.skiko_skiko"),
        "skiko-skottie" to directory.resolve("adaptation/org.jetbrains.skiko_skiko-skottie"),
    ))
    includeComposeUiSources(directory)
}

private fun Settings.includeComposeUiSources(directory: File) {
    val stateFile = directory.resolve("vendor/compose/state.json")
    check(stateFile.isFile) { "Source preparation did not produce vendor/compose/state.json" }
    val state = JsonSlurper().parse(stateFile) as Map<*, *>
    val repository = settingsDir.canonicalFile.toPath()
    for ((name, expected) in state["inputs"] as Map<*, *>) {
        val input = settingsDir.resolve(name as String).canonicalFile
        require(input.toPath().startsWith(repository)) { "Invalid prepared Compose input path: $name" }
        check(input.isFile && sha256(input) == expected) { "Prepared Compose input changed: $name; re-run prepareNativeSources" }
    }
    check(sha256(settingsDir.resolve("libs.versions.toml")) == state["catalogSha256"]) { "Prepared Compose build does not use the root version catalog" }
    val sourceDirectory = directory.resolve(state["build"] as String).canonicalFile
    require(sourceDirectory == directory.resolve("vendor/compose/workspace/native-desktop").canonicalFile) { "Invalid prepared Compose source directory" }
    check(sourceDirectory.isDirectory) { "Prepared Compose working tree is missing" }
    val lock = JsonSlurper().parse(directory.resolve("upstream/compose.json")) as Map<*, *>
    val modules = lock["modules"] as Map<*, *>
    val projects = modules.entries.associate { (artifact, value) ->
        val module = value as Map<*, *>
        val rule = "compose-$artifact"
        val moduleVersions = state["moduleVersions"] as? Map<*, *>
        gradle.extensions.extraProperties.set("love.yinlin.nativeBridge.version.$rule", (moduleVersions?.get(artifact) ?: state["version"]) as String)
        rule to ("${module["group"] ?: lock["group"]}:$artifact" to ":$artifact")
    }
    val adaptations = modules.entries.associate { (artifact, value) ->
        val module = value as Map<*, *>
        "compose-$artifact" to directory.resolve("adaptation/${module["adaptation"] ?: "org.jetbrains.compose.${module["family"] ?: "ui"}_$artifact"}")
    }
    extensions.getByType<NativeBridgeSourcesExtension>().include(sourceDirectory, "compose-ui", projects, adaptations)
}

private fun sha256(file: File): String = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
