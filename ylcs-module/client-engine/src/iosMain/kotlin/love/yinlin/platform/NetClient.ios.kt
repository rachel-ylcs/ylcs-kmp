package love.yinlin.platform

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinClientEngineConfig

actual object NetClient {
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

    actual val common: HttpClient by lazy { HttpClient(Darwin) {
        useEngine()
        useJson()
        useCommonTimeout()
    } }

    actual val file: HttpClient by lazy { HttpClient(Darwin) {
        useEngine()
        useJson()
        useFileTimeout()
    } }

    actual val sockets: HttpClient by lazy { HttpClient(Darwin) {
        useWebSockets()
    } }
}