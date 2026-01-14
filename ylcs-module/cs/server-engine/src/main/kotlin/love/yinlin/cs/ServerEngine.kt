package love.yinlin.cs

import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.application.serverConfig
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import love.yinlin.extension.Json
import love.yinlin.extension.Object
import love.yinlin.extension.makeObject
import love.yinlin.extension.parseJson
import love.yinlin.extension.to
import love.yinlin.cs.service.Database
import love.yinlin.cs.service.Redis
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KFunction0
import kotlin.time.Duration.Companion.seconds

abstract class ServerEngine {
    protected abstract val port: Int
    protected open val isCopyResources: Boolean = false
    protected open val loggerClass: KClass<out Any>? = null
    protected open val configFile: String? = null
    protected open val plugins: List<ServerPlugin> = emptyList()

    protected abstract val public: String
    protected abstract val APIScope.api: List<KFunction0<Unit>>
    protected open val proxy: Proxy? = null

    protected open val useDatabase: Boolean = false
    protected open val useRedis: Boolean = false

    protected open fun Database.onDatabaseCreate() { }
    protected open fun Redis.onRedisCreate() { }

    protected open fun onPrepare() { }
    protected open fun onStart() { }
    protected open fun onClose() { }

    private var config = makeObject { }

    private var database: Database? = null
    private var redis: Redis? = null

    private lateinit var loggerDelegate: Logger
    val logger: Logger get() = loggerDelegate

    fun run() {
        onPrepare()

        val classLoader = this::class.java.classLoader
        if (isCopyResources) copyResources(classLoader, public)
        configFile?.let {
            classLoader.getResourceAsStream(it)?.asSource()?.buffered()?.use { source ->
                source.readByteArray().decodeToString().parseJson.Object
            }?.let { obj -> config = obj }
        }

        if (useDatabase) {
            val db = Database(config["database"]!!.to())
            database = db
            db.onDatabaseCreate()
        }

        if (useRedis) {
            val rd = Redis(config["redis"]!!.to())
            redis = rd
            rd.onRedisCreate()
        }

        val customLogger = loggerClass?.java?.let { LoggerFactory.getLogger(it) }
        embeddedServer(
            factory = Netty,
            configure = {
                connectors += EngineConnectorBuilder().also {
                    it.port = port
                    it.host = "localhost"
                }
            },
            rootConfig = serverConfig(applicationEnvironment {
                if (customLogger != null) log = customLogger
                loggerDelegate = log
            }) {
                developmentMode = false
                module {
                    install(ContentNegotiation) { json(Json) }
                    install(WebSockets) {
                        pingPeriod = 15.seconds
                        timeout = 15.seconds
                        maxFrameSize = Long.MAX_VALUE
                        masking = false
                        contentConverter = KotlinxWebsocketSerializationConverter(Json)
                    }
                    for (plugin in plugins) {
                        install(plugin.instance)
                    }

                    routing {
                        staticFiles(public, File(public))

                        val scope = object : APIScope(this, log) {}
                        database?.let { if (useDatabase) scope.db = it }
                        redis?.let { if (useRedis) scope.redis = it }

                        scope.api.forEach { it() }
                        proxy?.apply { listen() }
                    }

                    onStart()
                }
            }
        ).start(wait = true)

        onClose()

        if (useDatabase) database?.close()
        if (useRedis) redis?.close()
    }
}