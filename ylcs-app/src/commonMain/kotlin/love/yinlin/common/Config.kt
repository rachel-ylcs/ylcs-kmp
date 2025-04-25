package love.yinlin.common

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateList
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.serializer
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.data.music.MusicPlaylist
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.extension.DateEx
import love.yinlin.extension.replaceAll
import love.yinlin.extension.toMutableStateMap
import love.yinlin.platform.KV
import love.yinlin.platform.getJson
import love.yinlin.platform.setJson
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Stable
class KVConfig(private val kv: KV) {
	companion object {
		const val UPDATE: Long = Long.MAX_VALUE
	}

	/* ----------------  持久化存储  ---------------- */

	@Stable
	private abstract class ValueState<T>(private val version: String? = null) : ReadWriteProperty<Any?, T> {
		abstract fun kvGet(key: String): T
		abstract fun kvSet(key: String, value: T)

		private val KProperty<*>.storageKey: String get() = "${this.name}${version}"
		private var state: MutableState<T>? = null

		final override fun getValue(thisRef: Any?, property: KProperty<*>): T {
			if (state == null) state = mutableStateOf(kvGet(property.storageKey))
			return state!!.value
		}

		final override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
			if (state == null) state = mutableStateOf(value)
			else state!!.value = value
			kvSet(property.storageKey, value)
		}
	}

	@Stable
	abstract inner class CollectionState<C, RC : C>(
		name: String,
		version: String? = null,
		private val serializer: KSerializer<C>,
		stateFactory: (C) -> RC,
		defaultFactory: () -> C
	) {
		protected val storageKey = "$name$version"
		protected val state: RC = stateFactory(kv.getJson(serializer, storageKey, defaultFactory))
		val items: C = state

		protected fun save() { kv.setJson(serializer, storageKey, state) }

		abstract val size: Int
	}

	@Stable
	open inner class ListState<T>(
		name: String,
		version: String?,
		itemSerializer: KSerializer<T>,
		defaultFactory: () -> List<T>
	) : CollectionState<List<T>, SnapshotStateList<T>>(
		name = name,
		version = version,
		serializer = ListSerializer(itemSerializer),
		stateFactory = { it.toMutableStateList() },
		defaultFactory = defaultFactory
	) {
		override val size: Int get() = state.size

		operator fun set(index: Int, item: T) {
			state[index] = item
			save()
		}

		operator fun get(index: Int): T = state[index]

		inline fun <R> map(transform: (T) -> R): List<R> = items.map(transform)

		operator fun iterator(): Iterator<T> = state.iterator()

		fun withIndex(): Iterable<IndexedValue<T>> = state.withIndex()

		inline fun contains(predicate: (T) -> Boolean): Boolean = items.find(predicate) != null

		operator fun plusAssign(item: T) {
			state += item
			save()
		}

		operator fun minusAssign(item: T) {
			state -= item
			save()
		}

		fun removeAll(predicate: (T) -> Boolean): Boolean = state.removeAll(predicate = predicate)

		fun replaceAll(items: List<T>) = state.replaceAll(items)
	}

	@Stable
	open inner class MapState<K, V>(
		name: String,
		version: String?,
		keySerializer: KSerializer<K>,
		valueSerializer: KSerializer<V>,
		defaultFactory: () -> Map<K, V>
	) : CollectionState<Map<K, V>, SnapshotStateMap<K, V>>(
		name = name,
		version = version,
		serializer = MapSerializer(keySerializer, valueSerializer),
		stateFactory = { it.toMutableStateMap() },
		defaultFactory = defaultFactory
	){
		override val size: Int get() = state.size

		operator fun set(key: K, value: V) {
			state[key] = value
			save()
		}

		operator fun get(key: K): V? = state[key]

		inline fun <R> map(transform: (K, V) -> R): List<R> = items.map { transform(it.key, it.value) }

		operator fun iterator(): Iterator<Map.Entry<K, V>> = state.iterator()

		operator fun plusAssign(item: Pair<K, V>) {
			state += item
			save()
		}

		operator fun plusAssign(items: Map<K, V>) {
			state.putAll(items)
			save()
		}

		operator fun minusAssign(key: K) {
			state.remove(key)
			save()
		}

		fun renameKey(key: K, newKey: K, block: (V) -> V = { it }) {
			val value = state.remove(key)
			if (value != null) {
				state += newKey to block(value)
				save()
			}
		}

		fun replaceAll(items: Map<K, V>) = state.replaceAll(items)
	}

	@Stable
	private class CacheState(private val kv: KV, private val default: Long = 0L) : ReadWriteProperty<Any?, Long> {
		private var state: MutableState<Long>? = null

		override fun getValue(thisRef: Any?, property: KProperty<*>): Long {
			if (state == null) state = mutableStateOf(kv.get(property.name, default))
			return state!!.value
		}

		override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
			val newCacheValue = DateEx.CurrentLong
			if (state == null) state = mutableStateOf(newCacheValue)
			else state!!.value = newCacheValue
			kv.set(property.name, newCacheValue)
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
		name = name,
		version = version,
		itemSerializer = serializer<T>(),
		defaultFactory = defaultFactory
	)

	private inline fun <reified K, reified V> mapState(
		name: String,
		version: String? = null,
		noinline defaultFactory: () -> Map<K, V> = { emptyMap() }
	) = MapState(
		name = name,
		version = version,
		keySerializer = serializer<K>(),
		valueSerializer = serializer<V>(),
		defaultFactory = defaultFactory
	)

	/* ------------------  微博  ------------------ */

	// 微博用户
	val weiboUsers = listState("weiboUsers") { WeiboUserInfo.DEFAULT }

	/* ------------------  听歌  ------------------ */

	// 歌单
	val playlistLibrary = mapState<String, MusicPlaylist>("playlistLibrary")
	// 上次播放列表
	var lastPlaylist by stringState("")
	// 上次播放歌曲
	var lastMusic by stringState("")
	// 播放模式
	var musicPlayMode: MusicPlayMode by jsonState { MusicPlayMode.ORDER }

	/* ------------------  社区  ------------------ */

	// 用户 Token
	var userToken: String by stringState("")
	// 用户 信息
	var userProfile: UserProfile? by jsonState { null }
	// 用户 头像缓存键
	var cacheUserAvatar: Long by CacheState(kv)
	// 用户 背景墙缓存键
	var cacheUserWall: Long by CacheState(kv)
}