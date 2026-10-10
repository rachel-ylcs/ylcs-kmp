package love.yinlin.encoding

enum class Encoding {
    UTF8,
    GBK;

    companion object {
        val Default: Encoding = UTF8
    }
}