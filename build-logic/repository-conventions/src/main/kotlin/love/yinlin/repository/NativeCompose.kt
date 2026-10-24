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
 * 按本机 `native.compose` 开关接入 Native Compose，命令行属性优先，默认关闭。
 * 在应用 `ylcs.repository` 后调用；源码和 SDK 仍由准备脚本显式生成。
 */
@Suppress("UnstableApiUsage")
fun Settings.nativeCompose() {
    val enabled = providers.gradleProperty("native.compose").getOrElse(localProperties["native.compose"] ?: "false").trim().toBoolean()
    if (!enabled) return

    pluginManager.apply("love.yinlin.native-bridge.settings")
    extensions.getByType<RepositorySettings>().modules {
        module("native-compose") {
            module("samples") {
                module("skiko-offscreen", "skiko-window")
            }
        }
    }

    val directory = settingsDir.resolve("native-compose")
    dependencyResolutionManagement.repositories.maven {
        name = "NativeVerification"
        setUrl(providers.gradleProperty("native.repository").getOrElse(directory.resolve("build/repository").absolutePath))
        content { includeGroup("love.yinlin.native.skiko") }
    }
    if (providers.gradleProperty("native.source").getOrElse("true").toBoolean()) includeNativeComposeSources(directory)
}

private fun Settings.includeNativeComposeSources(directory: File) {
    val stateFile = directory.resolve("vendor/state.json")
    if (!stateFile.isFile) {
        Logging.getLogger("native-compose").lifecycle("Native Skiko sources are not prepared; run python native-compose/scripts/sync_upstream.py and reload Gradle for source development.")
        return
    }

    val state = JsonSlurper().parse(stateFile) as Map<*, *>
    val fingerprint = MessageDigest.getInstance("SHA-256")
    val inputs = listOf("upstream/lock.json", "upstream/skiko-native-files.json", "skiko/adapt.py", "scripts/sync_upstream.py", "THIRD_PARTY_NOTICES.md") +
        directory.resolve("licenses").listFiles { file -> file.isFile && file.extension == "txt" }.orEmpty()
            .map { it.relativeTo(directory).invariantSeparatorsPath }.sorted()
    inputs.forEach { fingerprint.update(directory.resolve(it).readBytes()) }
    check(fingerprint.digest().joinToString("") { "%02x".format(it) } == state["fingerprint"]) {
        "Prepared Skiko sources are stale. Re-run python native-compose/scripts/sync_upstream.py."
    }

    val sourceDirectory = directory.resolve(state["build"] as String).canonicalFile
    require(sourceDirectory.toPath().startsWith(directory.resolve("vendor/prepared").canonicalFile.toPath())) { "Invalid prepared source directory" }
    val lock = JsonSlurper().parse(directory.resolve("upstream/lock.json")) as Map<*, *>
    extensions.getByType<NativeBridgeSourcesExtension>().include("skiko-core", sourceDirectory, "${lock["group"]}:skiko", "skiko")
}
