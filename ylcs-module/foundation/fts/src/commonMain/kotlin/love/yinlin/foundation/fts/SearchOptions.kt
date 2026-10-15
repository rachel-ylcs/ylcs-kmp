package love.yinlin.foundation.fts

data class SearchOptions(
    val mode: SearchMode = SearchMode.Contains,
    val ignoreCase: Boolean = true,
    val maxEditDistance: Int = 1,
    val limit: Int = Int.MAX_VALUE,
) {
    init {
        require(maxEditDistance >= 0) { "maxEditDistance must be non-negative" }
        require(limit >= 0) { "limit must be non-negative" }
    }
}
