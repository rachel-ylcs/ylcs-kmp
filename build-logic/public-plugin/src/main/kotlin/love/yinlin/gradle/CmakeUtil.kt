package love.yinlin.gradle

import java.io.File

private const val SharedCppSourceDirs = "RACHEL_CPP_SOURCE_DIRS"

internal fun cmakeCacheNeedsReset(build: File, source: File, executable: String, options: List<String>): Boolean {
    val cache = build.resolve("CMakeCache.txt")
    if (!cache.isFile) return false
    val paths = cache.useLines { lines ->
        lines.filter { (it.startsWith("CMAKE_") || it.startsWith("$SharedCppSourceDirs:")) && ':' in it && '=' in it }.associate {
            it.substringBefore(':') to it.substringAfter('=')
        }
    }
    // CMake caches paths found under these roots independently of the command-line option.
    val sourceDirs = options.lastOrNull { it.startsWith("-D$SharedCppSourceDirs=") }?.substringAfter('=').orEmpty()
    if (paths[SharedCppSourceDirs].orEmpty() != sourceDirs) return true

    val toolKeys = setOf("CMAKE_C_COMPILER", "CMAKE_CXX_COMPILER", "CMAKE_MAKE_PROGRAM")
    val tools = options.filter { it.startsWith("-D") && it.substringBefore('=').removePrefix("-D") in toolKeys }
        .associate { it.substringBefore('=').removePrefix("-D") to it.substringAfter('=') }
    val expected = mapOf("CMAKE_HOME_DIRECTORY" to source.absolutePath, "CMAKE_CACHEFILE_DIR" to build.absolutePath, "CMAKE_COMMAND" to executable) + tools
    return expected.any { (key, value) ->
        val previous = paths[key]
        previous != null && File(value).isAbsolute && File(previous).canonicalFile != File(value).canonicalFile
    }
}
