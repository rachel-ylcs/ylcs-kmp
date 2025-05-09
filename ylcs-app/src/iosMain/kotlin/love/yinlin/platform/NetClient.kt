package love.yinlin.platform

import androidx.compose.runtime.Stable
import io.ktor.client.*
import io.ktor.client.engine.darwin.*

actual object NetClient {
    private fun HttpClientConfig<DarwinClientEngineConfig>.useEngine() {
        engine {
            configureRequest {
                setAllowsCellularAccess(true)
            }
            configureSession {
                setAllowsCellularAccess(true)
            }
        }
    }

    @Stable
    actual val common: HttpClient = HttpClient(Darwin) {
        useEngine()
        useJson()
        useCommonTimeout()
    }

    @Stable
    actual val file: HttpClient = HttpClient(Darwin) {
        useEngine()
        useJson()
        useFileTimeout()
    }
}