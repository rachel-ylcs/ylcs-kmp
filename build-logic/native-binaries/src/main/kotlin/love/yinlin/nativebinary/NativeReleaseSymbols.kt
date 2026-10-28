package love.yinlin.nativebinary

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.SharedLibrary
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/** Shared by the standalone Native templates and the Compose bridge; Debug and linkable archives are untouched. */
object NativeReleaseSymbols {
    fun recordDistribution(binary: File, record: File) {
        record.parentFile.mkdirs()
        record.writeText("binary=${binary.name}\nsha256=${hash(binary)}\n")
    }

    fun configure(project: Project, target: KotlinNativeTarget) {
        if (target.konanTarget.name !in setOf("mingw_x64", "linux_x64", "macos_arm64", "macos_x64")) return
        val dependencies = File(project.providers.gradleProperty("konan.data.dir").orElse(project.providers.environmentVariable("KONAN_DATA_DIR")).getOrElse(File(System.getProperty("user.home"), ".konan").path), "dependencies")
        val objcopy = project.providers.gradleProperty("rachel.native.objcopy").orNull
        target.binaries.configureEach {
            val binary = this
            if (binary.buildType != NativeBuildType.RELEASE || binary !is Executable && binary !is SharedLibrary) return@configureEach
            val symbols = project.layout.buildDirectory.dir("nativeSymbols/${target.name}/${binary.name}")
            val platform = target.konanTarget.name
            binary.linkTaskProvider.configure {
                // Both plugins can see this task, possibly through different plugin class loaders.
                val marker = "love.yinlin.nativeReleaseSymbols"
                if (extensions.extraProperties.has(marker)) return@configure
                extensions.extraProperties.set(marker, true)
                outputs.dir(symbols).withPropertyName("nativeReleaseSymbols")
                inputs.property("nativeReleaseSymbolsFormat", 1)
                if (objcopy != null) inputs.property("nativeReleaseObjcopy", objcopy)
                doLast("separateNativeReleaseSymbols") {
                    separate(binary.outputFile, symbols.get().asFile, platform, dependencies, objcopy)
                    logger.lifecycle("Release symbols: ${symbols.get().asFile}")
                }
            }
        }
    }

    fun separate(binary: File, directory: File, platform: String, dependencies: File, objcopyOverride: String? = null) {
        require(binary.isFile) { "Missing linked Native binary: $binary" }
        directory.mkdirs()
        // Work beside the link output. An unsuccessful tool/validation never replaces the original.
        val work = Files.createTempDirectory(binary.parentFile.toPath(), ".release-symbols-").toFile()
        try {
            val originalHash = hash(binary)
            val stripped = work.resolve(binary.name)
            if (platform.startsWith("macos_")) {
                binary.copyTo(stripped)
                val uuid = machoUuid(binary)
                val exports = command(listOf("/usr/bin/xcrun", "nm", "-gU", binary.absolutePath))
                // Keep nlist symbols for atos even when an optimized Release has no DWARF.
                binary.copyTo(directory.resolve("${binary.name}.unstripped"), overwrite = true)
                val dsym = File(binary.path + ".dSYM")
                if (dsym.isDirectory) {
                    require(machoUuid(dsym) == uuid) { "Release dSYM does not match the linked binary: $dsym" }
                    dsym.copyRecursively(directory.resolve(dsym.name), overwrite = true)
                }
                // -x preserves external symbols/exports and dyld binding information in shared libraries.
                command(listOf("/usr/bin/xcrun", "strip", "-S", "-x", stripped.absolutePath))
                // Stripping invalidates the linker's ad-hoc Mach-O signature (including arm64).
                command(listOf("/usr/bin/codesign", "--force", "--sign", "-", stripped.absolutePath))
                command(listOf("/usr/bin/codesign", "--verify", stripped.absolutePath))
                require(machoUuid(stripped) == uuid) { "Stripping changed the Mach-O UUID" }
                require(command(listOf("/usr/bin/xcrun", "nm", "-gU", stripped.absolutePath)) == exports) { "Stripping changed Mach-O exports" }
            }
            else {
                val objcopy = objcopyOverride ?: findObjcopy(platform, dependencies)
                val debug = directory.resolve("${binary.name}.debug")
                command(listOf(objcopy, "--only-keep-debug", binary.absolutePath, work.resolve(debug.name).absolutePath))
                command(listOf(objcopy, "--strip-all", binary.absolutePath, stripped.absolutePath))
                if (platform == "mingw_x64") NativePeImage.verifyStripped(binary.readBytes(), stripped.readBytes())
                else {
                    // The basename and CRC let debuggers find the separate file after it is copied alongside the executable.
                    command(listOf(objcopy, "--add-gnu-debuglink=${work.resolve(debug.name).absolutePath}", stripped.absolutePath))
                }
                work.resolve(debug.name).copyTo(debug, overwrite = true)
            }
            require(stripped.isFile && stripped.length() > 0) { "Symbol stripping produced an empty binary: $binary" }
            stripped.setExecutable(binary.canExecute(), false)
            val strippedHash = hash(stripped)
            directory.resolve("symbols.properties").writeText("format=1\ntarget=$platform\nbinary=${binary.name}\nunstripped.sha256=$originalHash\nstripped.sha256=$strippedHash\n")
            // Same-filesystem atomic replacement; do not truncate a good binary when a tool fails.
            Files.move(stripped.toPath(), binary.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        }
        finally { work.deleteRecursively() }
    }

    private fun findObjcopy(platform: String, dependencies: File): String {
        val windows = System.getProperty("os.name").startsWith("Windows")
        val roots = dependencies.listFiles().orEmpty().filter { it.isDirectory }.sortedByDescending { it.name }
        if (!windows) {
            // This binutils is already downloaded for a normal Linux Native link.
            val bundled = roots.filter { it.name.startsWith("x86_64-unknown-linux-gnu-gcc-") }.map { it.resolve("bin/x86_64-unknown-linux-gnu-objcopy") }
            return bundled.firstOrNull { it.isFile && it.canExecute() }?.absolutePath ?: "objcopy"
        }
        val candidates = if (platform == "mingw_x64") roots.filter { it.name.startsWith("msys2-mingw-w64-") }.map { it.resolve("bin/objcopy.exe") }
        else roots.filter { it.name.startsWith("target-toolchain-") }.map { it.resolve("bin/llvm-objcopy.exe") }
        return candidates.firstOrNull { it.isFile }?.absolutePath
            ?: error("No Native $platform objcopy in $dependencies; set -Prachel.native.objcopy=/path/to/objcopy")
    }

    private fun machoUuid(file: File): Set<String> {
        val text = command(listOf("/usr/bin/xcrun", "dwarfdump", "--uuid", file.absolutePath))
        return Regex("UUID: ([A-Fa-f0-9-]+)").findAll(text).map { it.groupValues[1].uppercase() }.toSet().also {
            require(it.isNotEmpty()) { "Missing Mach-O UUID: $file" }
        }
    }

    private fun command(arguments: List<String>): String {
        val process = ProcessBuilder(arguments).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        check(process.waitFor() == 0) { "Native symbol tool failed: ${arguments.joinToString(" ")}\n$output" }
        return output
    }

    private fun hash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { stream ->
            val buffer = ByteArray(65536)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
