package love.yinlin.platform

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.Js
import io.ktor.client.engine.js.JsClientEngineConfig

private fun HttpClientConfig<JsClientEngineConfig>.useEngine() {
    engine {

    }
}

actual val internalCommon: HttpClient by lazy { HttpClient(Js) {
    useEngine()
    useJson()
    useCommonTimeout()
} }

actual val internalFile: HttpClient by lazy { HttpClient(Js) {
    useEngine()
    useJson()
    useFileTimeout()
} }

actual val internalSockets: HttpClient by lazy { HttpClient(Js) {
    useWebSockets()
} }