package love.yinlin.cs

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.application.serverConfig
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import love.yinlin.extension.Json
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
     * 端口
     */
    abstract val port: Int

    /**
     * 日志输出器
     */
    open val logger: ServerLogger = ServerLogger.Default

    /**
     * 插件
     */
    open val plugins: List<BaseServerPlugin<*, *>> = emptyList()

    /**
     * 准备
     */
    protected open fun onServerPrepare() { }

    /**
     * 清理
     */
    protected open fun onServerClose() { }

    /**
     * 运行
     */
    fun run() {
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
                    install(ContentNegotiation) { json(Json) }
                    for (plugin in plugins) {

                    }
                }
            }
        ).start(wait = true)

        // 清理
        onServerClose()
        logger.close()
    }
}