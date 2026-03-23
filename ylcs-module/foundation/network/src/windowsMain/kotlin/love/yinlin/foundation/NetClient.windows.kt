package love.yinlin.foundation

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

actual fun buildCommonNetClient(timeout: Long): NetClient = NetClient(HttpClient(WinHttp) {
    useEngine()
    useJson()
    useTimeout(timeout)
})

actual fun buildFileClient(timeout: Long): NetClient = NetClient(HttpClient(WinHttp) {
    useEngine()
    useJson()
    useTimeout(timeout)
})

actual fun buildSocketClient(): WebSocketClient = WebSocketClient(HttpClient(WinHttp) {
    useWebSockets()
})