package love.yinlin.gradle

import java.io.File

internal fun cmakeCacheNeedsReset(build: File, source: File, compilers: Map<String, String> = emptyMap()): Boolean {
    val cache = build.resolve("CMakeCache.txt")
    if (!cache.isFile) return false
    val paths = cache.useLines { lines ->
        lines.filter { it.startsWith("CMAKE_") && ':' in it && '=' in it }.associate {
            it.substringBefore(':') to it.substringAfter('=')
        }
    }
    val expected = mapOf("CMAKE_HOME_DIRECTORY" to source.absolutePath, "CMAKE_CACHEFILE_DIR" to build.absolutePath) + compilers
    return expected.any { (key, value) ->
        val previous = paths[key]
        previous != null && File(value).isAbsolute && File(previous).canonicalFile != File(value).canonicalFile
    }
}
