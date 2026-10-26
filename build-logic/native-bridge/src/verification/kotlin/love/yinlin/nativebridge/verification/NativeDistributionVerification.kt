package love.yinlin.nativebridge.verification

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/** Runs only when the maintainer explicitly requests the Gradle distribution verification task. */
internal class NativeDistributionVerification(private val nativeDirectory: File) {
    internal fun windowsImports(executable: File): List<String> {
        val bytes = executable.readBytes()
        fun number(offset: Int, size: Int): Long {
            check(offset >= 0 && offset <= bytes.size - size) { "Truncated Windows executable: $executable" }
            val buffer = ByteBuffer.wrap(bytes, offset, size).order(ByteOrder.LITTLE_ENDIAN)
            return if (size == 2) buffer.short.toLong() and 0xffff else buffer.int.toLong() and 0xffffffffL
        }
        val pe = number(0x3c, 4).toInt()
        check(number(pe, 4) == 0x4550L) { "Invalid Windows PE signature" }
        val sections = number(pe + 6, 2).toInt()
        val optionalSize = number(pe + 20, 2).toInt()
        val optional = pe + 24
        check(number(optional, 2) == 0x20bL && number(optional + 68, 2) == 2L) { "Expected an x64 GUI executable" }
        val importAddress = number(optional + 120, 4)
        val entries = (0 until sections).map { index ->
            val start = optional + optionalSize + index * 40
            Triple(number(start + 12, 4), maxOf(number(start + 8, 4), number(start + 16, 4)), number(start + 20, 4))
        }
        fun fileOffset(address: Long): Int {
            val section = entries.singleOrNull { address >= it.first && address < it.first + it.second } ?: error("Invalid PE import address")
            val offset = section.third + address - section.first
            check(offset in 0 .. bytes.lastIndex.toLong()) { "PE import address escapes the executable" }
            return offset.toInt()
        }
        if (importAddress == 0L) return emptyList()
        val imports = mutableListOf<String>()
        var position = fileOffset(importAddress)
        while ((0 until 5).any { number(position + it * 4, 4) != 0L }) {
            check(imports.size < 4096) { "Oversized PE import table" }
            val name = fileOffset(number(position + 12, 4))
            val end = (name .. minOf(bytes.lastIndex, name + 1024)).firstOrNull { bytes[it] == 0.toByte() } ?: error("Unterminated PE library name")
            imports.add(bytes.copyOfRange(name, end).toString(Charsets.US_ASCII))
            position += 20
        }
        return imports
    }

    private fun output(arguments: List<String>, directory: File, smoke: Boolean = false): String {
        val log = Files.createTempFile(nativeDirectory.resolve("vendor/build/distribution-verification").toPath(), "process-", ".log").toFile()
        try {
            val process = ProcessBuilder(arguments).directory(directory).redirectErrorStream(true).redirectOutput(log).apply {
                if (smoke) environment()["SDL_VIDEODRIVER"] = "dummy"
            }.start()
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                error("Native distribution process timed out: ${arguments.first()}")
            }
            check(log.length() <= 8L * 1024 * 1024) { "Native distribution log exceeds its limit" }
            val text = log.readText()
            check(process.exitValue() == 0) { "Native distribution process failed: ${arguments.first()}\n$text" }
            return text
        } finally { log.delete() }
    }

    fun verify(target: String) {
        check(target == hostTarget()) { "Run distributions on their matching Native host" }
        val targetName = targetSourceNames.getValue(target)
        val root = nativeDirectory.parentFile.resolve("ylcs-app/native-compose/build/nativeApplication/$targetName")
        val work = nativeDirectory.resolve("vendor/build/distribution-verification").apply { mkdirs() }
        for (kind in listOf("debug", "release")) {
            val source = root.resolve("${kind}Executable")
            check(source.isDirectory) { "Missing $kind application directory: $source" }
            val destination = Files.createTempDirectory(work.toPath(), "native compose relocated ").toFile()
            source.copyTreeTo(destination)
            val files = destination.walkTopDown().filter { it.isFile }.toList()
            check(files.none { it.name == "application.json" || it.name == "application-icon.ico" || ("sdl3" in it.name.lowercase() && listOf(".dll", ".so", ".dylib").any { extension -> extension in it.name.lowercase() }) }) { "Unexpected metadata or dynamic SDL in $source" }
            val executable = files.singleOrNull { it.name in setOf("native_compose", "native_compose.exe", "native_compose.kexe") } ?: error("Missing or ambiguous executable: $source")
            val libraries = when (target) {
                "mingw_x64" -> windowsImports(executable).joinToString("\n")
                "macos_arm64" -> output(listOf("otool", "-L", executable.path), destination)
                else -> output(listOf("readelf", "-d", executable.path), destination)
            }
            check("sdl3" !in libraries.lowercase()) { "Executable still depends on dynamic SDL: $libraries" }
            check(target == "mingw_x64" || Files.isExecutable(executable.toPath())) { "Executable permissions were lost: $executable" }
            check(files.any { "composeResources" in it.invariantSeparatorsPath }) { "Compose resources are missing: $source" }
            output(listOf(executable.path, "--smoke", "--software"), destination, smoke = true)
            println("PASS: relocated Native Compose $target/$kind: $destination")
        }
    }
}
