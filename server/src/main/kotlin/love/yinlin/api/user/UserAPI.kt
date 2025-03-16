package love.yinlin.api.user

import io.ktor.server.routing.Routing
import love.yinlin.DB
import love.yinlin.Redis
import love.yinlin.api.ImplMap
import love.yinlin.api.TokenExpireError
import love.yinlin.data.rachel.Comment
import love.yinlin.data.rachel.UserConstraint
import love.yinlin.platform.Platform
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
		throw ValidationError("If", args.joinToString(",")) else Unit
	fun throwName(vararg args: String) = if (!UserConstraint.checkName(*args))
		throw ValidationError("Name", args.joinToString(",")) else Unit
	fun throwId(vararg args: Number) = if (args.any { it.toLong() <= 0L })
		throw ValidationError("Id", args.joinToString(",")) else Unit
	fun throwPassword(vararg args: String) = if (!UserConstraint.checkPassword(*args))
		throw ValidationError("Password", args.joinToString(",")) else Unit
	fun throwEmpty(vararg args: String) = if (args.any { it.isEmpty() })
		throw ValidationError("Empty", args.joinToString(",")) else Unit
	fun throwSection(section: Int) = if (section !in Comment.Section.NOTIFICATION .. Comment.Section.DISCUSSION)
		throw ValidationError("Section", section) else Comment.Section.commentTable(section)
}

fun DB.throwGetUser(uid: Int, col: String = "uid") = this.throwQuerySQLSingle("SELECT $col FROM user WHERE uid = ?", uid)

/* ------------------ 鉴权 --------------------- */

data class Token(
	val uid: Int,
	val platform: Platform,
	val magic: Int = MAGIC,
	val timestamp: Long = System.currentTimeMillis()
) {
	companion object {
		const val MAGIC = 19911211

		fun fromBytes(bytes: ByteArray): Token? {
			val uidBytes = bytes.copyOfRange(0, 4)
			val magicBytes = bytes.copyOfRange(4, 8)
			val platformBytes = bytes.copyOfRange(8, 12)
			val timestampBytes = bytes.copyOfRange(12, 20)
			val uid = ByteBuffer.wrap(uidBytes).int
			val magic = ByteBuffer.wrap(magicBytes).int
			val platform = Platform.fromInt(ByteBuffer.wrap(platformBytes).int)
			val timestamp = ByteBuffer.wrap(timestampBytes).long
			return if (uid > 0 && magic == MAGIC && timestamp in 1727755860000..1917058260000 && platform != null)
				Token(uid = uid, platform = platform, timestamp = timestamp) else null
		}

		fun keys(uid: Int): List<String> = Platform.entries.map { "token/${it.ordinal}/$uid" }
	}

	val bytes: ByteArray by lazy {
		val buffer = ByteBuffer.allocate(Int.SIZE_BYTES * 3 + Long.SIZE_BYTES)
		buffer.putInt(uid)
		buffer.putInt(19911211)
		buffer.putInt(platform.ordinal)
		buffer.putLong(timestamp)
		buffer.array()
	}

	val key: String = "token/${platform.ordinal}/$uid"
}

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

	@Suppress("GetInstance")
	fun throwGenerateToken(token: Token): String {
		val cipher = Cipher.getInstance(KEY_ALGORITHM)
		cipher.init(Cipher.ENCRYPT_MODE, AES_KEY)
		val encryptedBytes = cipher.doFinal(token.bytes)
		val tokenString = Base64.getEncoder().encodeToString(encryptedBytes)
		Redis.use {
			it.setex(token.key, 7 * 24 * 60 * 60, tokenString)
		}
		return tokenString
	}

	@Suppress("GetInstance")
	private fun parseToken(tokenString: String): Token {
		val encryptedBytes = Base64.getDecoder().decode(tokenString)
		val cipher = Cipher.getInstance(KEY_ALGORITHM)
		cipher.init(Cipher.DECRYPT_MODE, AES_KEY)
		val bytes = cipher.doFinal(encryptedBytes)
		return Token.fromBytes(bytes)!!
	}

	fun throwExpireToken(tokenString: String): Int {
		val token = parseToken(tokenString)
		// keyToken 可能是 null 或 已经失效的 token
		val saveTokenString = Redis.use { it.get(token.key) }
		return if (saveTokenString == tokenString) token.uid else throw TokenExpireError(token.uid)
	}

	fun throwReGenerateToken(srcTokenString: String): String {
		val token = parseToken(srcTokenString)
		val saveTokenString = Redis.use { it.get(token.key) }
		return if (saveTokenString == srcTokenString) throwGenerateToken(Token(uid = token.uid, platform = token.platform))
			else throw TokenExpireError(token.uid)
	}

	fun removeToken(tokenString: String) {
		val token = parseToken(tokenString)
		val saveTokenString = Redis.use { it.get(token.key) }
		if (saveTokenString == tokenString) Redis.use { it.del(token.key) }
		else throw TokenExpireError(token.uid)
	}

	fun removeAllTokens(uid: Int) {
		if (uid > 0) {
			for (tokenString in Token.keys(uid)) {
				Redis.use { it.del(tokenString) }
			}
		}
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