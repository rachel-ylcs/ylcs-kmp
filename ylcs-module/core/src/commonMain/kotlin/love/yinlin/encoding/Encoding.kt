package love.yinlin.encoding

enum class Encoding {
    UTF8,
    ASCII,
    GBK;

    companion object {
        val Default: Encoding = UTF8
    }
}