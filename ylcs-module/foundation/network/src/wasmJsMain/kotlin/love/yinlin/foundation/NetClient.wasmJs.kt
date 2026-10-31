package love.yinlin.foundation

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.js.Js
import io.ktor.client.engine.js.JsClientEngineConfig
import io.ktor.client.plugins.compression.ContentEncoding

private fun HttpClientConfig<JsClientEngineConfig>.useEngine() {
    engine {

    }
}

private fun <T : HttpClientEngineConfig> HttpClientConfig<T>.useEncoding() {
    install(ContentEncoding) {
        deflate(1.0F)
        gzip(0.9F)
    }
}

actual fun buildCommonNetClient(timeout: Long): NetClient = NetClient(HttpClient(Js) {
    useRedirect()
    useEngine()
    useJson()
    useEncoding()
    useTimeout(timeout)
})

actual fun buildFileClient(timeout: Long): NetClient = NetClient(HttpClient(Js) {
    useRedirect()
    useEngine()
    useJson()
    useEncoding()
    useTimeout(timeout)
})

actual fun buildSocketClient(): WebSocketClient = WebSocketClient(HttpClient(Js) {
    useEncoding()
    useWebSockets()
})