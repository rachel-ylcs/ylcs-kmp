import groovy.json.JsonSlurper
import java.security.MessageDigest
import java.util.Properties

val nativeDirectory = settingsDir.resolve("../..").canonicalFile
val repository = nativeDirectory.parentFile
apply(from = repository.resolve("gradle/wrapper/check-version.settings.gradle.kts"))

val pointer = nativeDirectory.resolve("vendor/current.json")
check(pointer.isFile) { "Run :repository-conventions:prepareNativeSources before configuring Native verification." }
val state = JsonSlurper().parse(pointer) as Map<*, *>
val hash = state["sha256"] as String
val relative = state["directory"] as String
check(Regex("[a-f0-9]{64}").matches(hash) && relative == "vendor/g/${hash.take(24)}") { "Invalid Native generation pointer" }
val generation = nativeDirectory.resolve(relative).canonicalFile
val catalog = repository.resolve("libs.versions.toml")
fun sha256(file: File): String = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
val catalogHash = sha256(catalog)
check(generation.toPath().startsWith(nativeDirectory.toPath()) && state["catalogSha256"] == catalogHash &&
    JsonSlurper().parse(generation.resolve("ready.json")) == state) { "Prepare the current Native sources before running verification." }
val marker = JsonSlurper().parse(generation.resolve(".rachel-generation.json")) as Map<*, *>
check(marker["sha256"] == hash) { "The Native generation marker does not match the selected workspace." }
val inputs = state["inputs"] as? Map<*, *> ?: error("Native preparation input snapshot is missing. Prepare the current Native sources before running verification.")
for ((path, expected) in inputs) {
    val input = repository.resolve(path as String).canonicalFile
    check(input.toPath().startsWith(repository.toPath()) && input.isFile && sha256(input) == expected) {
        "Native input changed: $path. Prepare the current Native sources before running verification."
    }
}
gradle.extensions.extraProperties.set("love.yinlin.nativeCompose.verificationGeneration", generation)
val compilerProfile = Properties().apply { repository.resolve("gradle.properties").inputStream().use(::load) }
gradle.extensions.extraProperties.set("rachel.kotlin.compilerArgs", compilerProfile.getProperty("rachel.kotlin.compilerArgs").split(','))
