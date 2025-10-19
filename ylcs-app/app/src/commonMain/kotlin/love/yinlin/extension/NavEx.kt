package love.yinlin.extension

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import love.yinlin.common.uri.Uri

inline fun <reified T> buildNavType(
	isNullableAllowed: Boolean = false
): NavType<T> = object : NavType<T>(isNullableAllowed = isNullableAllowed) {
	override fun put(bundle: SavedState, key: String, value: T) = bundle.write { putString(key, value.toJsonString()) }
	override fun get(bundle: SavedState, key: String): T? = bundle.read { getString(key).parseJsonValue() }

	override fun parseValue(value: String): T = Uri.decodeUri(value).parseJsonValue()!!

	override fun serializeAsValue(value: T): String = Uri.encodeUri(value.toJsonString())
}

val navTypeCaches = mutableMapOf<String, NavType<*>>()

inline fun <reified T> getNavType(): NavType<*> {
	val name = T::class.qualifiedName!!
	val type = navTypeCaches[name]
	if (type == null) {
		val newType = buildNavType<T>()
		navTypeCaches[name] = newType
		return newType
	}
	else return type
}