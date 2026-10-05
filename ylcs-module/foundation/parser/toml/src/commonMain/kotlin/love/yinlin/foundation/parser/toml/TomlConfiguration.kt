package love.yinlin.foundation.parser.toml

import androidx.annotation.IntRange

data class TomlConfiguration(
    val version: TomlVersion = TomlVersion.V1_1,
    val style: TomlStyle = TomlStyle.Tables,
    @IntRange(1, 512) val maxDepth: Int = 128,
    val maxNodes: Int = 1000000,
) {
    init {
        require(maxDepth in 1..512) { "maxDepth must be between 1 and 512" }
        require(maxNodes > 0) { "maxNodes must be positive" }
    }
}