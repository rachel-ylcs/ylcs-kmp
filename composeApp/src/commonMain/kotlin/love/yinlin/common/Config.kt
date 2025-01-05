package love.yinlin.common

import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.platform.KV
import love.yinlin.platform.getJson
import love.yinlin.platform.setJson
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Config(kv: KV) {
	abstract class Value<T>(private val kv: KV, private val version: String? = null) : ReadWriteProperty<Any?, T> {
		abstract fun KV.kvGet(key: String): T
		abstract fun KV.kvSet(key: String, value: T)

		private fun key(property: KProperty<*>): String = if (version != null) "${property.name}_${version}" else property.name
		override fun getValue(thisRef: Any?, property: KProperty<*>): T = kv.kvGet(key(property))
		override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = kv.kvSet(key(property), value)
	}

	private fun KV.booleanValue(default: Boolean, version: String? = null) = object : Value<Boolean>(this, version) {
		override fun KV.kvGet(key: String) = this.get(key, default)
		override fun KV.kvSet(key: String, value: Boolean) { this.set(key, value) }
	}

	private fun KV.intValue(default: Int, version: String? = null) = object : Value<Int>(this, version) {
		override fun KV.kvGet(key: String) = this.get(key, default)
		override fun KV.kvSet(key: String, value: Int) { this.set(key, value) }
	}

	private fun KV.longValue(default: Long, version: String? = null) = object : Value<Long>(this, version) {
		override fun KV.kvGet(key: String) = this.get(key, default)
		override fun KV.kvSet(key: String, value: Long) { this.set(key, value) }
	}

	private fun KV.floatValue(default: Float, version: String? = null) = object : Value<Float>(this, version) {
		override fun KV.kvGet(key: String) = this.get(key, default)
		override fun KV.kvSet(key: String, value: Float) { this.set(key, value) }
	}

	private fun KV.doubleValue(default: Double, version: String? = null) = object : Value<Double>(this, version) {
		override fun KV.kvGet(key: String) = this.get(key, default)
		override fun KV.kvSet(key: String, value: Double) { this.set(key, value) }
	}

	private fun KV.stringValue(default: String, version: String? = null) = object : Value<String>(this, version) {
		override fun KV.kvGet(key: String) = this.get(key, default)
		override fun KV.kvSet(key: String, value: String) { this.set(key, value) }
	}

	private inline fun <reified T> KV.jsonValue(version: String? = null, crossinline defValueGetter: () -> T) = object : Value<T>(this, version) {
		override fun KV.kvGet(key: String): T = this.getJson(key, defValueGetter())
		override fun KV.kvSet(key: String, value: T) { this.setJson(key, value) }
	}

	// 微博用户
	val weiboUsers: List<WeiboUserInfo> by kv.jsonValue { WeiboUserInfo.DEFAULT_WEIBO_USER_INFO }
}