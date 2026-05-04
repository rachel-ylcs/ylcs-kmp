package love.yinlin.foundation

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import java.net.Proxy

private fun HttpClientConfig<OkHttpConfig>.useEngine() = engine {
    proxy = Proxy.NO_PROXY
    config {
        followRedirects(true)
        followSslRedirects(true)
    }
}

actual fun buildCommonNetClient(timeout: Long): NetClient = NetClient(HttpClient(OkHttp) {
    useEngine()
    useJson()
    useTimeout(timeout)
})

actual fun buildFileClient(timeout: Long): NetClient = NetClient(HttpClient(OkHttp) {
    useEngine()
    useJson()
    useTimeout(timeout)
})

actual fun buildSocketClient(): WebSocketClient = WebSocketClient(HttpClient(OkHttp) {
    useWebSockets()
})