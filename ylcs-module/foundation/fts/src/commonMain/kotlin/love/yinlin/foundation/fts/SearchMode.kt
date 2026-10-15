package love.yinlin.foundation.fts

// 字面模式在单个字段内匹配；词项模式将查询分词，各词项以 AND 连接，可以命中不同字段
enum class SearchMode {
    Contains, // 连续子串，包括空格和标点
    Exact, // 整个字段相等，包括空格和标点
    WholeWord, // 每个查询词都必须是索引中的完整词项
    Prefix, // 每个查询词都必须是某个索引词项的前缀
    Fuzzy; // 每个查询词与某个索引词项的 Levenshtein 编辑距离不超过阈值
}