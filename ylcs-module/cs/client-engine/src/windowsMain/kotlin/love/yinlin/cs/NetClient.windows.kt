package love.yinlin.cs

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.winhttp.WinHttp
import io.ktor.client.engine.winhttp.WinHttpClientEngineConfig
import io.ktor.http.HttpProtocolVersion

private fun HttpClientConfig<WinHttpClientEngineConfig>.useEngine() {
    engine {
        protocolVersion = HttpProtocolVersion.HTTP_1_1
    }
}

actual val internalCommon: HttpClient by lazy { HttpClient(WinHttp) {
    useEngine()
    useJson()
    useCommonTimeout()
} }

actual val internalFile: HttpClient by lazy { HttpClient(WinHttp) {
    useEngine()
    useJson()
    useFileTimeout()
} }

actual val internalSockets: HttpClient by lazy { HttpClient(WinHttp) {
    useWebSockets()
} }