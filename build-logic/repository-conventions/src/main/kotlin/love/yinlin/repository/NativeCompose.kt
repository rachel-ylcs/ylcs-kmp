package love.yinlin.repository

import groovy.json.JsonSlurper
import love.yinlin.nativebridge.NativeBridgeSourcesExtension
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logging
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.maven
import java.io.File
import java.security.MessageDigest

/**
 * `native.compose` 只控制移植源码维护，默认关闭；不控制业务模块是否支持 Native。
 * 开启后重现锁定源码并关联真实 project，普通消费者由桥插件按 target 选择 Maven 工件。
 */
@Suppress("UnstableApiUsage")
fun Settings.nativeCompose() {
    val value = providers.gradleProperty("native.compose").getOrElse(localProperties["native.compose"] ?: "false").trim().lowercase()
    require(value in setOf("true", "false")) { "native.compose must be true or false" }
    val enabled = value == "true"
    gradle.extensions.extraProperties.set("love.yinlin.nativeCompose.maintainer", enabled)

    // An explicit test/private repository is available to ordinary Maven consumers too.
    providers.gradleProperty("native.repository").orNull?.let { repository ->
        dependencyResolutionManagement.repositories.maven {
            name = "NativeVerification"
            setUrl(repository)
            content { includeGroup("love.yinlin.native.skiko") }
        }
    }
    if (!enabled) return
    require(!providers.gradleProperty("native.source").isPresent) {
        "native.source has been removed. native.compose=true always uses source projects; use native.compose=false for Maven consumption."
    }

    pluginManager.apply("love.yinlin.native-bridge.settings")
    extensions.getByType<RepositorySettings>().modules {
        module("native-compose") {
            module("adaptation") {
                module("samples") {
                    module("skiko")
                }
            }
        }
        module("ylcs-app") { module("native-compose") }
    }

    val directory = settingsDir.resolve("native-compose")
    includeNativeComposeSources(directory)
}

private fun Settings.includeNativeComposeSources(directory: File) {
    val preparation = providers.exec {
        workingDir(settingsDir)
        val arguments = mutableListOf(providers.gradleProperty("python.command").getOrElse("python"), directory.resolve("scripts/sync_upstream.py").absolutePath)
        if (gradle.startParameter.isOffline) arguments.add("--offline")
        commandLine(arguments)
        isIgnoreExitValue = true
    }
    val output = preparation.standardOutput.asText.get().trim()
    if (output.isNotEmpty()) Logging.getLogger("native-compose").lifecycle(output)
    check(preparation.result.get().exitValue == 0) {
        "Cannot prepare Native Compose source projects: ${preparation.standardError.asText.get().trim()}"
    }
    val stateFile = directory.resolve("vendor/skiko/state.json")
    check(stateFile.isFile) { "Source preparation did not produce vendor/skiko/state.json" }

    val state = JsonSlurper().parse(stateFile) as Map<*, *>
    val repository = settingsDir.canonicalFile.toPath()
    val inputs = state["inputs"] as Map<*, *>
    for ((name, expected) in inputs) {
        val input = settingsDir.resolve(name as String).canonicalFile
        require(input.toPath().startsWith(repository)) { "Invalid prepared input path: $name" }
        check(input.isFile && sha256(input) == expected) { "Prepared input changed: $name; re-run sync_upstream.py" }
    }
    check(sha256(settingsDir.resolve("libs.versions.toml")) == state["catalogSha256"]) { "Prepared build does not use the root version catalog" }
    val versions = state["versions"] as Map<*, *>
    gradle.extensions.extraProperties.set("love.yinlin.nativeCompose.root", directory)
    for (rule in listOf("skiko-core", "skiko-skottie")) {
        gradle.extensions.extraProperties.set("love.yinlin.nativeBridge.version.$rule", versions["skiko"] as String)
    }

    val sourceDirectory = directory.resolve(state["build"] as String).canonicalFile
    require(sourceDirectory.toPath().startsWith(directory.resolve("vendor/skiko/prepared").canonicalFile.toPath())) { "Invalid prepared source directory" }
    val lock = JsonSlurper().parse(directory.resolve("upstream/lock.json")) as Map<*, *>
    extensions.getByType<NativeBridgeSourcesExtension>().include(sourceDirectory, "skiko", mapOf(
        "skiko-core" to ("${lock["group"]}:skiko" to ":"),
        "skiko-skottie" to ("${lock["group"]}:skiko-skottie" to ":skiko-skottie"),
    ), mapOf(
        "skiko-core" to directory.resolve("adaptation/org.jetbrains.skiko_skiko"),
        "skiko-skottie" to directory.resolve("adaptation/org.jetbrains.skiko_skiko-skottie"),
    ))
}

private fun sha256(file: File): String = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
