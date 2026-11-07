package love.yinlin.gradle

import java.io.File
import java.util.Properties

internal class KonanMingwToolchain(properties: Properties, dependencies: File, host: String) {
    private val windowsHost = host == "mingw_x64"
    private val suffix = if (windowsHost) ".exe" else ""
    private val llvm = dependencies.resolve(properties.getProperty("llvm.$host.user"))
    private val sysroot = dependencies.resolve(properties.getProperty("toolchainDependency.mingw_x64"))
    private val clang = llvm.resolve("bin/clang$suffix")
    private val clangCpp = llvm.resolve("bin/clang++$suffix")
    private val archiver = llvm.resolve("bin/llvm-ar$suffix")
    private val windres = sysroot.resolve("bin/windres.exe")
    private val ranlib = sysroot.resolve("bin/ranlib.exe")
    private val triple = properties.getProperty("targetTriple.mingw_x64")
    val ready: Boolean get() = clang.isFile && clangCpp.isFile && archiver.isFile && sysroot.isDirectory && (!windowsHost || windres.isFile && ranlib.isFile)

    fun options(): List<String> {
        check(ready) { "Incomplete Kotlin/Native MinGW toolchain in $llvm and $sysroot" }
        val options = listOf(
            "-DCMAKE_SYSTEM_NAME=Windows", "-DCMAKE_SYSTEM_PROCESSOR=x86_64",
            "-DCMAKE_C_COMPILER=${clang.invariantSeparatorsPath}", "-DCMAKE_CXX_COMPILER=${clangCpp.invariantSeparatorsPath}",
            "-DCMAKE_C_COMPILER_TARGET=$triple", "-DCMAKE_CXX_COMPILER_TARGET=$triple",
            "-DCMAKE_SYSROOT=${sysroot.invariantSeparatorsPath}", "-DCMAKE_AR=${archiver.invariantSeparatorsPath}",
        )
        return if (windowsHost) options + listOf("-DCMAKE_RC_COMPILER=${windres.invariantSeparatorsPath}", "-DCMAKE_RANLIB=${ranlib.invariantSeparatorsPath}") else options
    }

    fun path(): String = listOf(llvm.resolve("bin").path, sysroot.resolve("bin").path, System.getenv("PATH").orEmpty()).joinToString(File.pathSeparator)
}
