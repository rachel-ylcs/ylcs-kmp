import io.ktor.util.logging.LogLevel
import kotlinx.coroutines.runBlocking
import love.yinlin.cs.*
import love.yinlin.cs.plugin.JsonPlugin
import love.yinlin.cs.plugin.WebSocketPlugin
import love.yinlin.cs.sockets.LyricsSocketsManager
import love.yinlin.fs.File
import kotlin.Long
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) = runBlocking {
    object : ServerEngine(args) {
        override val port: Int = 1211

        override val public: String = ServerRes.toString()

        override val logger: ServerLogger = ServerLogger(
            console = true,
            file = File(currentDirectory, "logs"),
            defaultLevel = LogLevel.INFO
        )

        override val plugins: List<BasicServerPlugin> = listOf(
            JsonPlugin,
            WebSocketPlugin(
                socketPingPeriod = 15.seconds,
                socketTimeout = 15.seconds,
                socketMaxFrameSize = Long.MAX_VALUE,
                socketMasking = false
            )
        )

        override val apiScope: ServerScope = ServerScope(this)

        override suspend fun onServerPrepare() {
            // 初始化鉴权密钥
            apiScope.AN.init()
            logger.info("Resource | Get user token secret key successfully.")
            // 初始化歌词游戏歌词表
            val librarySize = LyricsSocketsManager.initLibrary(File(currentDirectory, "lyrics_game.json"))
            logger.info("Resource | Lyrics game library initialized, size = $librarySize.")
        }
    }.run()
}