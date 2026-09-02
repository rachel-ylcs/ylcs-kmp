import love.yinlin.cs.BasicServerPlugin
import love.yinlin.cs.ServerEngine
import love.yinlin.cs.ServerLogger
import love.yinlin.cs.plugin.JsonPlugin
import love.yinlin.cs.plugin.WebSocketPlugin
import love.yinlin.fs.File
import kotlin.Long
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) = object : ServerEngine(args) {
    override val port: Int = 1211

    override val public: String = "public"

    override val logger: ServerLogger = ServerLogger(true, File(currentDirectory, "logs"))

    override val plugins: List<BasicServerPlugin> = listOf(
        JsonPlugin,
        WebSocketPlugin(
            socketPingPeriod = 15.seconds,
            socketTimeout = 15.seconds,
            socketMaxFrameSize = Long.MAX_VALUE,
            socketMasking = false
        )
    )

    override fun onServerPrepare() {
        logger.info("hello rachel!")
    }
}.run()