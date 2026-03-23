package love.yinlin.foundation

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.CIOEngineConfig

private fun HttpClientConfig<CIOEngineConfig>.useEngine() {
    engine {

    }
}

actual fun buildCommonNetClient(timeout: Long): NetClient = NetClient(HttpClient(CIO) {
    useEngine()
    useJson()
    useTimeout(timeout)
})

actual fun buildFileClient(timeout: Long): NetClient = NetClient(HttpClient(CIO) {
    useEngine()
    useJson()
    useTimeout(timeout)
})

actual fun buildSocketClient(): WebSocketClient = WebSocketClient(HttpClient(CIO) {
    useWebSockets()
})