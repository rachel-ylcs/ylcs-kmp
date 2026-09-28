package love.yinlin.foundation.cryptography

import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray

@OptIn(ExperimentalWasmJsInterop::class)
private fun webCryptoRandomValues(size: Int): Int8Array = js("globalThis.crypto.getRandomValues(new Int8Array(size))")

internal actual fun secureRandomBytes(size: Int): ByteArray = webCryptoRandomValues(size).toByteArray()