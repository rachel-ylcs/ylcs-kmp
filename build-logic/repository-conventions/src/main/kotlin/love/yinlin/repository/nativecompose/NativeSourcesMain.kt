package love.yinlin.repository.nativecompose

import java.io.File

object NativeSourcesMain {
    @JvmStatic
    fun main(arguments: Array<String>) {
        require(arguments.size in 2 .. 3) { "Expected command and native-compose directory" }
        val directory = File(arguments[1]).canonicalFile
        when (arguments[0]) {
            "prepare" -> NativeUpstreamSync(directory).prepare(arguments.getOrNull(2) == "offline")
            "updates" -> NativeUpstreamSync(directory).checkUpdates()
            "cache-create" -> NativeSourceCache(directory).create()
            "cache-restore" -> NativeSourceCache(directory).restore()
            else -> error("Unknown Native source command: ${arguments[0]}")
        }
    }
}
