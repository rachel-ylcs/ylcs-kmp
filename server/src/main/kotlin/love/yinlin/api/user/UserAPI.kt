package love.yinlin.api.user

import io.ktor.server.routing.Routing
import love.yinlin.DB
import love.yinlin.Redis
import love.yinlin.api.ImplMap
import love.yinlin.api.TokenExpireError
import love.yinlin.data.rachel.Comment
import love.yinlin.data.rachel.UserConstraint
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

object VN {
	class ValidationError(source: String, data: Any? = null) : Throwable() {
		override val message: String = "ValidationError $source $data"
	}

	fun throwIf(vararg args: Boolean) = if (args.any { it })
		throw ValidationError("If") else Unit
	fun throwName(vararg args: String) = if (!UserConstraint.checkName(*args))
		throw ValidationError("Name", args) else Unit
	fun throwId(vararg args: Number) = if (args.any { it.toLong() <= 0L })
		throw ValidationError("Id", args) else Unit
	fun throwPassword(vararg args: String) = if (!UserConstraint.checkPassword(*args))
		throw ValidationError("Password", args) else Unit
	fun throwEmpty(vararg args: String) = if (args.any { it.isEmpty() })
		throw ValidationError("Empty", args) else Unit
	fun throwSection(section: Int) = if (section !in Comment.Section.NOTIFICATION .. Comment.Section.DISCUSSION)
		throw ValidationError("Section", section) else Comment.Section.commentTable(section)
}

fun DB.throwGetUser(uid: Int, col: String = "uid") = this.throwQuerySQLSingle("SELECT $col FROM user WHERE uid = ?", uid)

/* ------------------ 鉴权 --------------------- */

object AN {
	private const val TOKEN_SECRET_KEY = "token_secret_key"
	private const val KEY_ALGORITHM = "AES"

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
			val generator = KeyGenerator.getInstance(KEY_ALGORITHM)
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

	@Suppress("GetInstance")
	private fun tokenParseUid(token: String): Int = try {
		val encryptedBytes = Base64.getDecoder().decode(token)
		val cipher = Cipher.getInstance(KEY_ALGORITHM)
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

	@Suppress("GetInstance")
	fun throwGenerateToken(uid: Int): String {
		val timestamp = System.currentTimeMillis()
		val buffer = ByteBuffer.allocate(Int.SIZE_BYTES + Int.SIZE_BYTES + Long.SIZE_BYTES)
		buffer.putInt(uid)
		buffer.putInt(19911211)
		buffer.putLong(timestamp)
		val cipher = Cipher.getInstance(KEY_ALGORITHM)
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

fun Routing.userAPI(implMap: ImplMap) {
	accountAPI(implMap)
	activityAPI(implMap)
	backupAPI(implMap)
	infoAPI(implMap)
	mailAPI(implMap)
	profileAPI(implMap)
	topicAPI(implMap)
}