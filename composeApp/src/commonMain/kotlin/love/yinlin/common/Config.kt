package love.yinlin.common

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import kotlinx.datetime.Clock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.serializer
import love.yinlin.data.rachel.UserProfile
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.platform.KV
import love.yinlin.platform.getJson
import love.yinlin.platform.setJson
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class KVConfig(private val kv: KV) {
	/* ----------------  持久化存储  ---------------- */

	abstract class ValueState<T>(private val version: String? = null) : ReadWriteProperty<Any?, T> {
		abstract fun kvGet(key: String): T
		abstract fun kvSet(key: String, value: T)

		private fun key(property: KProperty<*>): String = "${property.name}${version}"
		private var state: MutableState<T>? = null

		final override fun getValue(thisRef: Any?, property: KProperty<*>): T {
			if (state == null) state = mutableStateOf(kvGet(key(property)))
			return state!!.value
		}

		final override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
			if (state == null) state = mutableStateOf(value)
			else state!!.value = value
			kvSet(key(property), value)
		}
	}

	class CacheState(private val kv: KV) : ReadWriteProperty<Any?, Long> {
		companion object {
			const val UPDATE: Long = Long.MAX_VALUE
		}

		private var state: MutableState<Long>? = null

		override fun getValue(thisRef: Any?, property: KProperty<*>): Long {
			if (state == null) state = mutableStateOf(kv.get(property.name, 0L))
			return state!!.value
		}

		override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
			val newCacheValue = Clock.System.now().toEpochMilliseconds()
			if (state == null) state = mutableStateOf(newCacheValue)
			else state!!.value = newCacheValue
			kv.set(property.name, newCacheValue)
		}
	}

	open inner class ListState<T>(
		itemSerializer: KSerializer<T>,
		name: String,
		version: String?,
		defaultFactory: () -> List<T>
	) {
		private val listSerializer = ListSerializer(itemSerializer)
		private val key = "$name$version"
		protected val state = kv.getJson(listSerializer, key, defaultFactory).toMutableStateList()

		val items: List<T> get() = state

		inline fun <R> map(transform: (T) -> R) = items.map(transform)
		operator fun iterator() = state.iterator()
		fun withIndex() = state.withIndex()
		fun contains(predicate: (T) -> Boolean): Boolean = state.find(predicate) != null

		operator fun set(index: Int, item: T) {
			state[index] = item
			kv.setJson(listSerializer, key, items)
		}

		operator fun plusAssign(item: T) {
			state += item
			kv.setJson(listSerializer, key, items)
		}

		operator fun minusAssign(item: T) {
			state -= item
			kv.setJson(listSerializer, key, items)
		}
	}

	private fun booleanState(
		default: Boolean,
		version: String? = null
	) = object : ValueState<Boolean>(version) {
		override fun kvGet(key: String): Boolean = kv.get(key, default)
		override fun kvSet(key: String, value: Boolean) { kv.set(key, value) }
	}

	private fun intState(
		default: Int,
		version: String? = null
	) = object : ValueState<Int>(version) {
		override fun kvGet(key: String) = kv.get(key, default)
		override fun kvSet(key: String, value: Int) { kv.set(key, value) }
	}

	private fun longState(
		default: Long,
		version: String? = null
	) = object : ValueState<Long>(version) {
		override fun kvGet(key: String) = kv.get(key, default)
		override fun kvSet(key: String, value: Long) { kv.set(key, value) }
	}

	private fun floatState(
		default: Float,
		version: String? = null
	) = object : ValueState<Float>(version) {
		override fun kvGet(key: String) = kv.get(key, default)
		override fun kvSet(key: String, value: Float) { kv.set(key, value) }
	}

	private fun doubleState(
		default: Double,
		version: String? = null
	) = object : ValueState<Double>(version) {
		override fun kvGet(key: String) = kv.get(key, default)
		override fun kvSet(key: String, value: Double) { kv.set(key, value) }
	}

	private fun stringState(
		default: String,
		version: String? = null
	) = object : ValueState<String>(version) {
		override fun kvGet(key: String) = kv.get(key, default)
		override fun kvSet(key: String, value: String) { kv.set(key, value) }
	}

	private inline fun <reified T> jsonState(
		version: String? = null,
		crossinline defaultFactory: () -> T
	) = object : ValueState<T>(version) {
		override fun kvGet(key: String): T = kv.getJson(key, defaultFactory)
		override fun kvSet(key: String, value: T) { kv.setJson(key, value) }
	}

	private inline fun <reified T> listState(
		name: String,
		version: String? = null,
		noinline defaultFactory: () -> List<T> = { emptyList() }
	) = ListState(
		itemSerializer = serializer<T>(),
		name = name,
		version = version,
		defaultFactory = defaultFactory
	)

	/* ------------------  配置  ------------------ */

	// 微博用户
	val weiboUsers = listState("weiboUsers", "2") { WeiboUserInfo.DEFAULT }

	// 用户 Token
	var userToken: String by stringState("")
	// 用户 信息
	var userProfile: UserProfile? by jsonState { null }
	// 用户 头像缓存键
	var cacheUserAvatar: Long by CacheState(kv)
	// 用户 背景墙缓存键
	var cacheUserWall: Long by CacheState(kv)
}