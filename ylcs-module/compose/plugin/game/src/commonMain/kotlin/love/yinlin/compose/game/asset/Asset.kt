package love.yinlin.compose.game.asset

import androidx.compose.runtime.Stable
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@Stable
sealed class Asset<T : Any, I : Any>(internal val version: Int?) : ReadOnlyProperty<Any?, AssetDelegate<T>> {
    internal abstract val type: String
    internal abstract suspend fun build(input: I): T

    internal val delegate = AssetDelegate<T>()
    final override fun getValue(thisRef: Any?, property: KProperty<*>): AssetDelegate<T> {
        if (delegate.name == null) delegate.name = property.name
        return delegate
    }
}