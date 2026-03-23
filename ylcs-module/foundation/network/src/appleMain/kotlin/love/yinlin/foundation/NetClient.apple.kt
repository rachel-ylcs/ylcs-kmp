package love.yinlin.foundation

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinClientEngineConfig

private fun HttpClientConfig<DarwinClientEngineConfig>.useEngine() {
    engine {
        configureRequest {
            setAllowsCellularAccess(true)
        }
        configureSession {
            setAllowsCellularAccess(true)
        }
    }
}

actual fun buildCommonNetClient(timeout: Long): NetClient = NetClient(HttpClient(Darwin) {
    useEngine()
    useJson()
    useTimeout(timeout)
})

actual fun buildFileClient(timeout: Long): NetClient = NetClient(HttpClient(Darwin) {
    useEngine()
    useJson()
    useTimeout(timeout)
})

actual fun buildSocketClient(): WebSocketClient = WebSocketClient(HttpClient(Darwin) {
    useWebSockets()
})