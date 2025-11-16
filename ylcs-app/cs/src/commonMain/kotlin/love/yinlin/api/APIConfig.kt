package love.yinlin.api

data object APIConfig {
    const val PROXY_NAME = "proxy"
    const val MIN_PAGE_NUM = 20
    const val MAX_PAGE_NUM = 30
    val Int.coercePageNum: Int get() = this.coerceAtLeast(MIN_PAGE_NUM).coerceAtMost(MAX_PAGE_NUM)
}