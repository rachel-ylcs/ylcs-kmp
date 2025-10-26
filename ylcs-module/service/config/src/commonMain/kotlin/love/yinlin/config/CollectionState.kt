package love.yinlin.config

import androidx.compose.runtime.Stable
import kotlinx.serialization.KSerializer
import love.yinlin.platform.getJson
import love.yinlin.platform.setJson
import love.yinlin.startup.StartupKV

@Stable
abstract class CollectionState<C, RC : C>(
    private val kv: StartupKV,
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
}