package love.yinlin.compose.config

import androidx.compose.runtime.Stable
import kotlinx.serialization.KSerializer
import love.yinlin.kv.KV
import love.yinlin.kv.getJson
import love.yinlin.kv.setJson

@Stable
abstract class CollectionState<C, RC : C>(
    private val kv: KV,
    name: String,
    version: String? = null,
    private val serializer: KSerializer<C>,
    stateFactory: (C) -> RC,
    defaultFactory: () -> C
) : ConfigState {
    protected val storageKey = "$name$version"
    protected val state: RC = stateFactory(kv.getJson(serializer, storageKey, defaultFactory))
    val items: C = state

    protected fun save() { kv.setJson(serializer, storageKey, state) }

    abstract val size: Int
    abstract val isEmpty: Boolean
    abstract val isNotEmpty: Boolean
}