package love.yinlin.encoding

enum class Encoding {
    UTF8,
    ASCII,
    LATIN1,
    GBK,
    UTF16LE,
    UTF16BE,
    UTF32LE,
    UTF32BE;

    companion object {
        val Default: Encoding = UTF8
    }
}
