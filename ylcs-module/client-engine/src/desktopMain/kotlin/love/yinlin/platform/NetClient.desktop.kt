package love.yinlin.platform

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import java.net.Proxy

private fun HttpClientConfig<OkHttpConfig>.useEngine() {
    engine {
        proxy = Proxy.NO_PROXY
        config {
            followRedirects(true)
            followSslRedirects(true)
        }
    }
}

actual val internalCommon: HttpClient by lazy { HttpClient(OkHttp) {
    useEngine()
    useJson()
    useCommonTimeout()
} }

actual val internalFile: HttpClient by lazy { HttpClient(OkHttp) {
    useEngine()
    useJson()
    useFileTimeout()
} }

actual val internalSockets: HttpClient by lazy { HttpClient(OkHttp) {
    useWebSockets()
} }