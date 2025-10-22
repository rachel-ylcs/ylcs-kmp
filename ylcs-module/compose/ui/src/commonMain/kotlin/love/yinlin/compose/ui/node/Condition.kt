package love.yinlin.compose.ui.node

import androidx.compose.ui.Modifier

inline fun Modifier.condition(value: Boolean, callback: Modifier.() -> Modifier): Modifier =
	if (value) this.callback() else this

inline fun Modifier.condition(value: Boolean, ifTrue: Modifier.() -> Modifier, ifFalse: Modifier.() -> Modifier): Modifier =
	if (value) this.ifTrue() else this.ifFalse()