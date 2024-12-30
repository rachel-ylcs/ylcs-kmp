package love.yinlin.screen

import kotlinx.serialization.Serializable

sealed interface Route {
	@Serializable
	data object Main : Route
}