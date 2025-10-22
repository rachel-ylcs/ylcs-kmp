package love.yinlin.compose.screen

inline fun <reified S : Screen<Unit>> route(): String = "route.${S::class.qualifiedName!!}"