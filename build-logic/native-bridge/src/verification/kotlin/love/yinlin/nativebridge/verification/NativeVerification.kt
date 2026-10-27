package love.yinlin.nativebridge.verification

import java.io.File

/** Maintainer verification is compiled Kotlin, outside the published bridge implementation. */
object NativeVerification {
    @JvmStatic
    fun main(arguments: Array<String>) {
        require(arguments.size == 2) { "Expected verification command and native-compose directory" }
        val directory = File(arguments[1]).canonicalFile
        check(directory.name == "native-compose" && directory.resolve("upstream").isDirectory)
        val verifier = NativePublicationVerification(directory)
        when (arguments[0]) {
            "sources" -> verifier.checkSources()
            "host" -> verifier.verifyHost(directory.resolve("vendor/build/repository"), System.getenv("NATIVE_TARGET") ?: hostTarget())
            "assemble" -> {
                val inputs = directory.parentFile.resolve(System.getenv("NATIVE_REPOSITORIES_DIR") ?: "host-publications")
                verifier.assemble(targetSuffixes.keys.associateWith { inputs.resolve("native-compose-maven-$it") }, directory.resolve("vendor/build/assembled-repository"))
            }
            else -> error("Unknown Native verification command: ${arguments[0]}")
        }
    }
}
