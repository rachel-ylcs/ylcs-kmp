package love.yinlin.api

import io.ktor.server.routing.Routing
import love.yinlin.Redis
import love.yinlin.throwQuerySQLSingle
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.ByteBuffer
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/* ------------------ 验证 --------------------- */

object Validation {
	class ValidationError(source: String, data: Any? = null) : Throwable() {
		override val message: String = "ValidationError $data"
	}

	const val MIN_NAME_LENGTH = 2
	const val MAX_NAME_LENGTH = 16
	const val MIN_PWD_LENGTH = 6
	const val MAX_PWD_LENGTH = 18
	const val RENAME_COIN_COST = 5

	fun throwIf(vararg args: Boolean) = if (args.any { it })
		throw ValidationError("If") else Unit
	fun throwName(vararg args: String) = if (args.any { it.length !in MIN_NAME_LENGTH .. MAX_NAME_LENGTH })
		throw ValidationError("Name", args) else Unit
	fun throwId(vararg args: Number) = if (args.any { it.toLong() <= 0L })
		throw ValidationError("Id", args) else Unit
	fun throwPassword(vararg args: String) = if (args.any { it.length !in MIN_PWD_LENGTH .. MAX_PWD_LENGTH })
		throw ValidationError("Password", args) else Unit
	fun throwEmpty(vararg args: String) = if (args.any { it.isEmpty() })
		throw ValidationError("Empty", args) else Unit
	fun throwGetUser(uid: Int, col: String = "uid") = throwQuerySQLSingle("SELECT $col FROM user WHERE uid = ?", uid)
}

/* ------------------ 鉴权 --------------------- */

object Authorization {
	private const val TOKEN_SECRET_KEY = "token_secret_key"

	private val AES_KEY: SecretKey = Redis.use {
		val keyString = it.get(TOKEN_SECRET_KEY)
		if (keyString != null) {
			val bis = ByteArrayInputStream(Base64.getDecoder().decode(keyString))
			val ois = ObjectInputStream(bis)
			val secretKey = ois.readObject() as SecretKey
			ois.close()
			secretKey
		}
		else {
			val generator = KeyGenerator.getInstance("AES")
			generator.init(256)
			val secretKey = generator.generateKey()
			val bos = ByteArrayOutputStream()
			val oos = ObjectOutputStream(bos)
			oos.writeObject(secretKey)
			oos.flush()
			oos.close()
			it.set(TOKEN_SECRET_KEY, Base64.getEncoder().encodeToString(bos.toByteArray()))
			secretKey
		}
	}

	private fun tokenKey(uid: Int): String = "token/${uid}"

	private fun tokenParseUid(token: String): Int = try {
		val encryptedBytes = Base64.getDecoder().decode(token)
		val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
		cipher.init(Cipher.DECRYPT_MODE, AES_KEY)
		val bytes = cipher.doFinal(encryptedBytes)
		val uidBytes = bytes.copyOfRange(0, 4)
		val timestampBytes = bytes.copyOfRange(8, 16)
		val uid = ByteBuffer.wrap(uidBytes).int
		val timestamp = ByteBuffer.wrap(timestampBytes).long
		if (uid > 0 && timestamp in 1727755860000..1917058260000) uid
		else 0
	}
	catch (_: Throwable) { 0 }

	fun throwGenerateToken(uid: Int): String {
		val timestamp = System.currentTimeMillis()
		val buffer = ByteBuffer.allocate(Int.SIZE_BYTES + Int.SIZE_BYTES + Long.SIZE_BYTES)
		buffer.putInt(uid)
		buffer.putInt(19911211)
		buffer.putLong(timestamp)
		val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
		cipher.init(Cipher.ENCRYPT_MODE, AES_KEY)
		val encryptedBytes = cipher.doFinal(buffer.array())
		val token = Base64.getEncoder().encodeToString(encryptedBytes)
		Redis.use {
			it.setex(tokenKey(uid), 7 * 24 * 60 * 60, token)
		}
		return token
	}

	fun throwExpireToken(token: String): Int {
		val uid = tokenParseUid(token)
		if (uid > 0) {
			// keyToken 可能是 null 或 已经失效的 token
			val keyToken = Redis.use {
				it.get(tokenKey(uid))
			}
			if (keyToken == token) return uid
		}
		throw TokenExpireError(uid)
	}

	fun removeToken(uid: Int): Boolean {
		val key = tokenKey(uid)
		return Redis.use {
			if (it.exists(key)) {
				it.del(key)
				true
			}
			else false
		}
	}

	fun throwRemoveToken(token: String) {
		val uid = tokenParseUid(token)
		if (uid <= 0 || !removeToken(uid)) throw TokenExpireError(uid)
	}
}

/* ------------------ 接口 --------------------- */

fun Routing.userAPI(implMap: ImplMap) {
	catchAsyncPost("/user/login") {
		data class Body(val name: String, val pwd: String)
		val (name, pwd) = call.to<Body>()

	}
}