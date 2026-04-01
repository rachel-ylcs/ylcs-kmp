package love.yinlin.foundation

import kotlin.properties.ReadOnlyProperty

fun interface StartupDelegate<S : Startup> : ReadOnlyProperty<Any?, S>
fun interface StartupNullableDelegate<S : Startup> : ReadOnlyProperty<Any?, S?>