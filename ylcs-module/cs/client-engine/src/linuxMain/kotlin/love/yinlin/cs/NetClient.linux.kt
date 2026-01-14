package love.yinlin.cs

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.CIOEngineConfig

private fun HttpClientConfig<CIOEngineConfig>.useEngine() {
    engine {

    }
}

actual val internalCommon: HttpClient by lazy { HttpClient(CIO) {
    useEngine()
    useJson()
    useCommonTimeout()
} }

actual val internalFile: HttpClient by lazy { HttpClient(CIO) {
    useEngine()
    useJson()
    useFileTimeout()
} }

actual val internalSockets: HttpClient by lazy { HttpClient(CIO) {
    useWebSockets()
} }