package love.yinlin.cs

import io.ktor.server.application.serverConfig
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import kotlinx.serialization.json.JsonObject
import love.yinlin.extension.Object
import love.yinlin.extension.catching
import love.yinlin.extension.makeObject
import love.yinlin.extension.parseJson
import love.yinlin.foundation.PlatformContext
import love.yinlin.fs.File
import love.yinlin.fs.PlatformFileSystem

/**
 * 服务器引擎
 */
abstract class ServerEngine(cmdLine: Array<String>) {
    val args = buildMap {
        for (arg in cmdLine) {
            if (arg.startsWith("--")) {
                val eqIndex = arg.indexOf('=')
                if (eqIndex <= 2) continue
                val key = arg.substring(2, eqIndex)
                val value = arg.substring(eqIndex + 1)
                put(key, value)
            }
        }
    }

    val currentDirectory = args["cd"]?.ifEmpty { null }?.let { File(it) } ?: PlatformFileSystem.appPath(PlatformContext.Instance, "")

    /**
     * 配置
     */
    var config: JsonObject = makeObject {  }
        private set

    /**
     * 端口
     */
    abstract val port: Int

    /**
     * 公共目录
     */
    abstract val public: String

    /**
     * 日志输出器
     */
    open val logger: ServerLogger = ServerLogger.Default

    /**
     * 插件
     */
    open val plugins: List<BasicServerPlugin> = emptyList()

    /**
     * 接口
     */
    protected abstract val apiScope: APIScope

    /**
     * 准备
     */
    protected open suspend fun onServerPrepare() { }

    /**
     * 启动
     */
    protected open suspend fun onServerStart() { }

    /**
     * 清理
     */
    protected open suspend fun onServerClose() { }

    /**
     * 运行
     */
    suspend fun run() {
        // 日志目录
        val logPath = File(currentDirectory, "logs")
        if (!logPath.exists()) logPath.mkdir()
        logger.info("Server is running in $currentDirectory")

        // 读取配置
        val configFile = File(currentDirectory, "config.json")
        catching { config = configFile.readText()!!.parseJson.Object }
        logger.info("Config file is loaded.")

        // 静态目录
        val staticFilePath = File(currentDirectory, public)
        if (!staticFilePath.exists()) staticFilePath.mkdir()
        logger.info("static file path is created.")

        // 接口作用域
        val scope = apiScope
        for (service in scope.services) service.onStart()

        // 初始准备
        onServerPrepare()

        // 启动服务器
        embeddedServer(
            factory = CIO,
            configure = {
                // 配置端口和主机
                connectors += EngineConnectorBuilder().also {
                    it.port = port
                    it.host = "localhost"
                }
            },
            rootConfig = serverConfig(applicationEnvironment {
                log = logger
            }) {
                developmentMode = false

                module {
                    // 插件
                    for (plugin in plugins) {
                        with(plugin) { onInstall() }
                    }

                    // 路由
                    routing {
                        // 静态目录
                        staticFiles(public, staticFilePath)
                        // 接口
                        scope.setupAPI(this)
                    }

                    onServerStart()
                }
            }
        ).start(wait = true)

        // 清理
        onServerClose()
        for (service in scope.services.asReversed()) service.onClose()
        logger.close()
    }
}