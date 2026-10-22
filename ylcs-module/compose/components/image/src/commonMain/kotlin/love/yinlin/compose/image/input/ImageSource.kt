package love.yinlin.compose.image.input

import androidx.compose.runtime.Stable
import io.ktor.http.decodeURLPart
import love.yinlin.compose.image.cache.ImageDiskCache
import love.yinlin.fs.File
import love.yinlin.platform.Platform
import love.yinlin.platform.platform

/** 图片来源。网络地址与版本键分离，本地路径里的 ?、# 不会被当作版本参数删除。 */
@Stable
interface ImageSource {
    @Stable
    class Network(val url: String, headers: Map<String, String> = emptyMap()) : ImageSource {
        val headers: Map<String, String> = headers.toMap()
        private val key = lazy { WebImageInput.buildCacheKey(this) }
        internal val contentKey: String get() = key.value
        internal val hasContentKey: Boolean get() = key.isInitialized()

        override fun equals(other: Any?): Boolean = other is Network && url == other.url && headers == other.headers
        override fun hashCode(): Int = 31 * url.hashCode() + headers.hashCode()
    }

    /** 本地文件复用内存结果；文件内容更新后需改变版本 key。 */
    @Stable
    data class LocalFile(val file: File) : ImageSource

    /** composeResources/ 开头的资源路径；可用 resource() 从 Res.getUri() 转换。 */
    @Stable
    data class Resource(val path: String) : ImageSource {
        init {
            require(path.startsWith("composeResources/") && path.split('/').none { it == ".." })
        }
    }

    /** 复制输入字节，调用方随后修改原数组不会改变请求或缓存内容。 */
    @Stable
    class Bytes(bytes: ByteArray) : ImageSource {
        internal val data = bytes.copyOf()
        private val key = lazy { ImageDiskCache.imageCacheHash(data) }
        internal val contentKey: String get() = key.value
        internal val hasContentKey: Boolean get() = key.isInitialized()
    }

    companion object {
        private fun imageFileUriPath(uri: String, windows: Boolean): String {
            val path = uri.substringAfter(':').removePrefix("//")
            require(path.startsWith('/')) { "Only local file URIs are supported" }

            val decoded = path.decodeURLPart()
            // file:///C:/... 的首个斜线是 URI 语法，Windows 盘符路径不能保留它
            return if (windows && decoded.length >= 3 && decoded[1].isLetter() && decoded[2] == ':') decoded.substring(1) else decoded
        }

        fun resource(path: String): Resource {
            val start = path.indexOf("composeResources/")
            require(start >= 0) { "Expected a Compose resource path" }
            return Resource(path.substring(start).decodeURLPart())
        }

        fun from(uri: String): ImageSource = when {
            uri.startsWith("https://", true) || uri.startsWith("http://", true) -> Network(uri)
            uri.startsWith("composeResources/") || uri.startsWith("file:///compose_resource/") || (uri.startsWith("file:///android_asset/") && uri.contains("/composeResources/")) || (uri.startsWith("jar:") && uri.contains("/composeResources/")) -> resource(uri)
            uri.startsWith("file:", true) -> LocalFile(File(imageFileUriPath(uri, platform in [Platform.Windows, Platform.WindowsNative])))
            else -> LocalFile(File(uri))
        }
    }
}
