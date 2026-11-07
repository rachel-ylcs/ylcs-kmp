package love.yinlin.nativebridge.verification

import java.io.File

/** Maintainer verification is compiled Kotlin, outside the published bridge implementation. */
object NativeVerification {
    @JvmStatic
    fun main(arguments: Array<String>) {
        require(arguments.size == 2) { "Expected verification command and native-compose directory" }
        val directory = File(arguments[1]).canonicalFile
        check(directory.name == "native-compose" && directory.resolve("upstream").isDirectory)
        val generation = if (arguments[0] == "sources") null else {
            val state = directory.resolve("vendor/current.json").readJson()
            val relative = state["directory"] as String
            val hash = state["sha256"] as String
            check(Regex("[a-f0-9]{64}").matches(hash) && relative == "vendor/g/${hash.take(24)}") { "Invalid Native generation pointer" }
            val selected = directory.contained(relative)
            check(selected.resolve("ready.json").readJson() == state &&
                state["catalogSha256"] == directory.parentFile.resolve("libs.versions.toml").digest("SHA-256")) { "Prepare the current Native sources first" }
            selected
        }
        val verifier = NativePublicationVerification(directory, generation)
        when (arguments[0]) {
            "sources" -> verifier.checkSources()
            "host" -> verifier.verifyHost(checkNotNull(generation).resolve("build/repository"), System.getenv("NATIVE_TARGET") ?: hostTarget())
            "assemble" -> {
                val inputs = directory.parentFile.resolve(System.getenv("NATIVE_REPOSITORIES_DIR") ?: "host-publications")
                verifier.assemble(targetSuffixes.keys.associateWith { inputs.resolve("native-compose-maven-$it") }, checkNotNull(generation).resolve("build/assembled-repository"))
            }
            else -> error("Unknown Native verification command: ${arguments[0]}")
        }
    }
}
