import love.yinlin.cs.ServerEngine
import love.yinlin.cs.ServerLogger
import love.yinlin.fs.File

fun main(args: Array<String>) = object : ServerEngine(args) {
    override val port: Int = 1211

    override val logger: ServerLogger = ServerLogger(true, File(currentDirectory, "logs"))

    override fun onServerPrepare() {
        logger.info("hello rachel!")
    }
}.run()