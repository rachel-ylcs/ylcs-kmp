package love.yinlin.platform

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import java.net.Proxy

object DesktopClient {
	private fun HttpClientConfig<OkHttpConfig>.useEngine() {
		engine {
			proxy = Proxy.NO_PROXY
			config {
				followRedirects(true)
				followSslRedirects(true)
			}
		}
	}

	val common: HttpClient = HttpClient(OkHttp) {
		useEngine()
		useJson()
		useCommonTimeout()
	}

	val file: HttpClient = HttpClient(OkHttp) {
		useEngine()
		useFileTimeout()
	}
}