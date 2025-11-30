package love.yinlin.compose.game

import androidx.compose.runtime.Stable

@Stable
data class AssetKey(val name: String, val version: Int? = null)