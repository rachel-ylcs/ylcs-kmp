package love.yinlin.platform

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import java.net.Proxy

actual object NetClient {
    private fun HttpClientConfig<OkHttpConfig>.useEngine() {
        engine {
            proxy = Proxy.NO_PROXY
            config {
                followRedirects(true)
                followSslRedirects(true)
            }
        }
    }

    actual val common: HttpClient = HttpClient(OkHttp) {
        useEngine()
        useJson()
        useCommonTimeout()
    }

    actual val file: HttpClient = HttpClient(OkHttp) {
        useEngine()
        useJson()
        useFileTimeout()
    }
}