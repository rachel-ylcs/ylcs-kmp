package love.yinlin.foundation.fts

/**
 * 索引与查询使用同一个分词器。自定义实现应保持确定性，返回非空词项。
 * 仅影响 WholeWord、Prefix 和 Fuzzy；字面及正则模式始终搜索原始字段。
 */
fun interface SearchTokenizer {
    fun tokenize(text: String): List<String>

    companion object {
        /**
         * 连续 Unicode 字母、数字、组合标记和下划线构成一个词项。
         * 不拆分 camelCase，不进行中文语义分词；中文片段可以直接使用 Contains。
         */
        val Default: SearchTokenizer = SearchTokenizer { text ->
            buildList {
                scanDefaultWords(text) {
                    add(it)
                    true
                }
            }
        }

        // 与 Kotlin Char.equals(ignoreCase = true) 使用相同的一对一折叠，保留 UTF-16 长度。
        // String.lowercase() 会展开某些字符，也不能统一 final sigma 等字符。
        internal fun foldCharacter(char: Char): Char = when {
            char in 'A' .. 'Z' -> (char.code or 32).toChar()
            char.code < 128 -> char
            else -> char.uppercaseChar().lowercaseChar()
        }

        private fun isWordCharacter(char: Char): Boolean {
            val code = char.code
            if (code < 128) return (code or 32) - 'a'.code in 0 .. 25 || char in '0' .. '9' || char == '_'
            return char.isLetterOrDigit() || when (char.category) {
                CharCategory.NON_SPACING_MARK, CharCategory.COMBINING_SPACING_MARK, CharCategory.ENCLOSING_MARK -> true
                else -> false
            }
        }

        /** 默认分词器内部使用回调，索引及查询不分配临时词项列表。false 提前停止扫描。 */
        internal inline fun scanDefaultWords(text: String, action: (String) -> Boolean): Boolean {
            var start = -1
            for (index in text.indices) {
                if (isWordCharacter(text[index])) {
                    if (start < 0) start = index
                }
                else if (start >= 0) {
                    if (!action(text.substring(start, index))) return false
                    start = -1
                }
            }
            return start < 0 || action(if (start == 0) text else text.substring(start))
        }

        internal fun foldCase(text: String): String {
            val first = text.indexOfFirst { it != foldCharacter(it) }
            if (first < 0) return text
            return buildString(text.length) {
                append(text, 0, first)
                for (index in first ..< text.length) append(foldCharacter(text[index]))
            }
        }
    }
}