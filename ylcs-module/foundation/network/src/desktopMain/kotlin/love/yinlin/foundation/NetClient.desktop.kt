package love.yinlin.foundation

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.plugins.compression.ContentEncoding
import java.net.Proxy

private fun HttpClientConfig<OkHttpConfig>.useEngine() {
    engine {
        proxy = Proxy.NO_PROXY
    }
}

private fun <T : HttpClientEngineConfig> HttpClientConfig<T>.useEncoding() {
    install(ContentEncoding) {
        deflate(1.0F)
        gzip(0.9F)
    }
}

actual fun buildCommonNetClient(timeout: Long): NetClient = NetClient(HttpClient(OkHttp) {
    useRedirect()
    useEngine()
    useJson()
    useEncoding()
    useTimeout(timeout)
})

actual fun buildFileClient(timeout: Long): NetClient = NetClient(HttpClient(OkHttp) {
    useRedirect()
    useEngine()
    useJson()
    useEncoding()
    useTimeout(timeout)
})

actual fun buildSocketClient(): WebSocketClient = WebSocketClient(HttpClient(OkHttp) {
    useEncoding()
    useWebSockets()
})