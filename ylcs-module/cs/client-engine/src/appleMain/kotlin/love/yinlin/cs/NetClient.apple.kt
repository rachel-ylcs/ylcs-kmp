package love.yinlin.cs

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

actual val internalCommon: HttpClient by lazy { HttpClient(Darwin) {
    useEngine()
    useJson()
    useCommonTimeout()
} }

actual val internalFile: HttpClient by lazy { HttpClient(Darwin) {
    useEngine()
    useJson()
    useFileTimeout()
} }

actual val internalSockets: HttpClient by lazy { HttpClient(Darwin) {
    useWebSockets()
} }