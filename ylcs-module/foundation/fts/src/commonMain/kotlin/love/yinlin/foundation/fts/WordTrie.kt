package love.yinlin.foundation.fts

/** 扁平数组 Trie；词典已排序，因此构建时不需要每个节点分配 Map。 */
internal class WordTrie(words: List<String>) {
    private val chars: CharArray
    private val children: IntArray
    private val siblings: IntArray
    private val terminals: IntArray
    private val maxDepth = words.maxOfOrNull { it.length } ?: 0
    private val wordCount = words.size

    init {
        var count = 1
        var previous = ""
        for (text in words) {
            var shared = 0
            while (shared < minOf(previous.length, text.length) && previous[shared] == text[shared]) ++shared
            count += text.length - shared
            previous = text
        }
        chars = CharArray(count)
        children = IntArray(count) { -1 }
        siblings = IntArray(count) { -1 }
        terminals = IntArray(count) { -1 }
        val path = IntArray(maxDepth + 1)
        val lastChild = IntArray(maxDepth + 1) { -1 }
        previous = ""
        var next = 1
        words.forEachIndexed { word, text ->
            var shared = 0
            while (shared < minOf(previous.length, text.length) && previous[shared] == text[shared]) ++shared
            for (index in shared ..< text.length) {
                val parent = path[index]
                chars[next] = text[index]
                if (lastChild[index] < 0) children[parent] = next else siblings[lastChild[index]] = next
                lastChild[index] = next
                lastChild[index + 1] = -1
                path[index + 1] = next++
            }
            terminals[path[text.length]] = word
            previous = text
        }
    }

    fun search(query: String, distance: Int, onMatch: (Int) -> Unit) {
        if (distance >= maxOf(query.length, maxDepth)) {
            for (word in 0 ..< wordCount) onMatch(word)
            return
        }
        val matcher = EditDistance(query, distance)
        // 路径深度不超过 query.length + distance，不为短查询按词典最长词项分配空间。
        val depthLimit = minOf(maxDepth.toLong(), query.length.toLong() + distance).toInt()
        if ((depthLimit.toLong() + 1) * matcher.width <= 4096) searchByDepth(matcher, depthLimit, distance, onMatch)
        else searchByBranch(matcher, depthLimit, distance, onMatch)
    }

    // 短查询的 DP 工作集小，按深度直接定位行，避免保存分支行的复制开销
    private fun searchByDepth(matcher: EditDistance, depthLimit: Int, distance: Int, onMatch: (Int) -> Unit) {
        val rows = IntArray((depthLimit + 1) * matcher.width)
        matcher.initial().copyInto(rows)
        val path = IntArray(depthLimit + 1)
        var node = children[0]
        var depth = 1
        while (node >= 0) {
            path[depth] = node
            val offset = depth * matcher.width
            val minimum = matcher.advance(chars[node], depth, rows, offset - matcher.width, offset)
            if (terminals[node] >= 0 && matcher.accepts(rows, depth, offset)) onMatch(terminals[node])
            if (minimum <= distance && depth < depthLimit && children[node] >= 0) {
                node = children[node]
                ++depth
            }
            else {
                while (depth > 0 && siblings[path[depth]] < 0) --depth
                if (depth == 0) break
                node = siblings[path[depth]]
            }
        }
    }

    private fun searchByBranch(matcher: EditDistance, depthLimit: Int, distance: Int, onMatch: (Int) -> Unit) {
        var branchNodes = IntArray(minOf(depthLimit, 8))
        var branchDepths = IntArray(branchNodes.size)
        var branchCount = 0
        var rows = IntArray(matcher.width * minOf(depthLimit + 2, 4))
        val initial = matcher.initial()
        initial.copyInto(rows)
        var parentOffset = 0
        var node = children[0]
        var depth = 1
        // 仅在有待访问兄弟节点时保存父行；单链任意长也只使用两个工作行。
        while (node >= 0) {
            if (siblings[node] >= 0) {
                if (branchCount == branchNodes.size) {
                    branchNodes = branchNodes.copyOf(maxOf(1, branchNodes.size * 2))
                    branchDepths = branchDepths.copyOf(branchNodes.size)
                }
                val snapshot = branchCount * matcher.width
                if (rows.size < snapshot + matcher.width) rows = rows.copyOf(maxOf(rows.size * 2, snapshot + matcher.width))
                if (snapshot != parentOffset) rows.copyInto(rows, snapshot, parentOffset, parentOffset + matcher.width)
                parentOffset = snapshot
                branchNodes[branchCount] = siblings[node]
                branchDepths[branchCount++] = depth
            }
            val firstWorkRow = branchCount * matcher.width
            val offset = if (parentOffset == firstWorkRow) firstWorkRow + matcher.width else firstWorkRow
            if (rows.size < offset + matcher.width) rows = rows.copyOf(maxOf(rows.size * 2, offset + matcher.width))
            val minimum = matcher.advance(chars[node], depth, rows, parentOffset, offset)
            if (terminals[node] >= 0 && matcher.accepts(rows, depth, offset)) onMatch(terminals[node])
            if (minimum <= distance && depth < depthLimit && children[node] >= 0) {
                node = children[node]
                ++depth
                parentOffset = offset
            }
            else {
                if (branchCount == 0) break
                --branchCount
                node = branchNodes[branchCount]
                depth = branchDepths[branchCount]
                parentOffset = branchCount * matcher.width
            }
        }
    }
}
