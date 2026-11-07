package love.yinlin.repository.nativecompose

import java.io.File

object NativeSourcesMain {
    @JvmStatic
    fun main(arguments: Array<String>) {
        require(arguments.size in 1 .. 2 && (arguments.size == 1 || arguments[1] == "offline")) { "Expected native-compose directory and optional offline flag" }
        NativeUpstreamSync(File(arguments[0])).prepare(arguments.size == 2)
    }
}
