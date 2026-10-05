package love.yinlin.foundation.parser.yaml

import androidx.annotation.IntRange

data class YamlConfiguration(
    val style: YamlStyle = YamlStyle.Block,
    @IntRange(1, 9) val indent: Int = 2,
    val blockStrings: Boolean = true,
    val allowDuplicateKeys: Boolean = false,
    val mergeKeys: Boolean = true,
    @IntRange(1, 1024) val maxDepth: Int = 128,
    val maxAliases: Int = 1000,
    val maxNodes: Int = 1000000,
) {
    init {
        require(indent in 1..9) { "indent must be between 1 and 9" }
        require(maxDepth in 1..1_024) { "maxDepth must be between 1 and 1024" }
        require(maxAliases >= 0) { "maxAliases must not be negative" }
        require(maxNodes > 0) { "maxNodes must be positive" }
    }
}