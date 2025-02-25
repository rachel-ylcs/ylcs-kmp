package love.yinlin.extension

import androidx.core.bundle.Bundle
import androidx.core.uri.UriUtils
import androidx.navigation.NavType
import kotlin.reflect.KType
import kotlin.reflect.typeOf

inline fun <reified T> buildNavType(
	isNullableAllowed: Boolean = false
): NavType<T> = object : NavType<T>(isNullableAllowed = isNullableAllowed) {
	override fun put(bundle: Bundle, key: String, value: T) = bundle.putString(key, value.toJsonString())
	override fun get(bundle: Bundle, key: String): T? = bundle.getString(key)?.parseJsonValue()
	override fun parseValue(value: String): T = UriUtils.decode(value).parseJsonValue()!!
	override fun serializeAsValue(value: T): String = UriUtils.encode(value.toJsonString())
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

inline fun <reified T> buildNavTypeMap() = mutableMapOf(
	typeOf<T>() to getNavType<T>()
)

inline fun <reified T> MutableMap<KType, NavType<*>>.appendNavType(): MutableMap<KType, NavType<*>> {
	put(typeOf<T>(), getNavType<T>())
	return this
}