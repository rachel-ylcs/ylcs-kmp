package love.yinlin.foundation

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.winhttp.WinHttp
import io.ktor.client.engine.winhttp.WinHttpClientEngineConfig
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.http.HttpProtocolVersion
import love.yinlin.foundation.compression.Gzip
import love.yinlin.foundation.compression.Zlib
import love.yinlin.foundation.http.compression.NativeContentEncoder

private fun HttpClientConfig<WinHttpClientEngineConfig>.useEngine() {
    engine {
        protocolVersion = HttpProtocolVersion.HTTP_1_1
    }
}

private fun <T : HttpClientEngineConfig> HttpClientConfig<T>.useEncoding() {
    install(ContentEncoding) {
        customEncoder(NativeContentEncoder("deflate", Zlib()), 1.0F)
        customEncoder(NativeContentEncoder("gzip", Gzip()), 0.9F)
    }
}

actual fun buildCommonNetClient(timeout: Long): NetClient = NetClient(HttpClient(WinHttp) {
    useRedirect()
    useEngine()
    useJson()
    useEncoding()
    useTimeout(timeout)
})

actual fun buildFileClient(timeout: Long): NetClient = NetClient(HttpClient(WinHttp) {
    useRedirect()
    useEngine()
    useJson()
    useEncoding()
    useTimeout(timeout)
})

actual fun buildSocketClient(): WebSocketClient = WebSocketClient(HttpClient(WinHttp) {
    useEncoding()
    useWebSockets()
})