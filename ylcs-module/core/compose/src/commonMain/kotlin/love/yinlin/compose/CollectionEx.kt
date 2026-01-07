package love.yinlin.compose

import androidx.compose.runtime.snapshots.SnapshotStateMap

//  ----------  Data Transfer  ----------

fun <K, V> Map<K, V>.toMutableStateMap() = SnapshotStateMap<K, V>().also { it.putAll(this) }