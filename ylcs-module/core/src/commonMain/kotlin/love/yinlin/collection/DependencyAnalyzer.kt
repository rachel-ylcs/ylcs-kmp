package love.yinlin.collection

/**
 * 依赖分析器
 *
 * 为集合拓扑排序分析依赖关系生成按依赖有序列表和依赖图
 *
 * @param K 唯一依赖键
 * @param V 值
 * @param items 待排序的集合
 * @param keyProvider 键值提供器
 * @param dependenciesProvider 依赖提供器
 */
class DependencyAnalyzer<K : Any, V>(
    items: Iterable<V>,
    keyProvider: (V) -> K,
    dependenciesProvider: (V) -> Iterable<K>
) {
    class UnknownDependencyError(val key: String, val dependentKey: String) : Exception("$key needs an unknown dependency on $dependentKey")
    class LoopDependencyError : Exception("loop dependency error")

    /**
     * 依赖顺序正确的列表
     *
     * 可以按照序号递增参与构造任务，序号递减参与析构任务
     * 在并发任务中可以根据依赖图依次await
     */
    val result: List<V>
    val dependenciesMap: Map<K, List<K>>

    init {
        // 依赖图
        val rawDependenciesMap = mutableMapOf<K, List<K>>()
        // 入度表
        val inDegree = mutableMapOf<K, Int>()
        // 邻接表
        val adjacencyList = mutableMapOf<K, MutableList<K>>()
        // 键值图
        val keyMap = items.associateBy(keyProvider)

        // 初始化入度表
        for ((key, value) in keyMap) {
            val filterDependencies = dependenciesProvider(value).mapNotNull { dependentKey ->
                // 未知依赖
                if (dependentKey !in keyMap) throw UnknownDependencyError(key.toString(), dependentKey.toString())
                // 排除自身依赖
                if (dependentKey != key) dependentKey else null
            }

            rawDependenciesMap[key] = filterDependencies
            inDegree[key] = filterDependencies.size

            for (dependentKey in filterDependencies) {
                adjacencyList.getOrPut(dependentKey, ::mutableListOf).add(key)
            }

            if (filterDependencies.isEmpty()) inDegree[key] = 0
        }

        // 初始化队列
        val queue = ArrayDeque<K>()
        for ((key, degree) in inDegree) {
            if (degree == 0) queue += key
        }

        val rawResult = mutableListOf<V>()

        // BFS
        while (queue.isNotEmpty()) {
            val currentKey = queue.removeAt(0)
            rawResult += keyMap[currentKey] ?: throw UnknownDependencyError(currentKey.toString(), "")

            adjacencyList[currentKey]?.forEach { dependentKey ->
                val updatedDegree = (inDegree[dependentKey] ?: 0) - 1
                inDegree[dependentKey] = updatedDegree
                if (updatedDegree == 0) queue += dependentKey
            }
        }

        // 循环依赖
        if (rawResult.size != keyMap.size) throw LoopDependencyError()

        result = rawResult
        dependenciesMap = rawDependenciesMap
    }
}