package love.yinlin.cs.user

import love.yinlin.cs.UnauthorizedException
import love.yinlin.cs.service.Redis
import love.yinlin.platform.Platform
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlin.io.encoding.Base64

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
            val platform = Platform.entries.getOrNull(ByteBuffer.wrap(platformBytes).int)
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

    private lateinit var AES_KEY: SecretKey
    private lateinit var redis: Redis

    fun initAESKey(redis: Redis) {
        this.redis = redis
        val keyString = redis[TOKEN_SECRET_KEY]
        AES_KEY = if (keyString != null) {
            val bis = ByteArrayInputStream(Base64.decode(keyString))
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
            redis[TOKEN_SECRET_KEY] = Base64.encode(bos.toByteArray())
            secretKey
        }
    }

    @Suppress("GetInstance")
    fun throwGenerateToken(token: Token): String {
        val cipher = Cipher.getInstance(KEY_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, AES_KEY)
        val encryptedBytes = cipher.doFinal(token.bytes)
        val tokenString = Base64.encode(encryptedBytes)
        redis.setex(token.key, tokenString, 30 * 24 * 60 * 60L)
        return tokenString
    }

    @Suppress("GetInstance")
    private fun parseToken(tokenString: String): Token {
        val encryptedBytes = Base64.decode(tokenString)
        val cipher = Cipher.getInstance(KEY_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, AES_KEY)
        val bytes = cipher.doFinal(encryptedBytes)
        return Token.fromBytes(bytes)!!
    }

    fun throwExpireToken(tokenString: String): Int {
        val token = parseToken(tokenString)
        // keyToken 可能是 null 或 已经失效的 token
        val saveTokenString = redis[token.key]
        return if (saveTokenString == tokenString) token.uid
        else if (saveTokenString.isNullOrEmpty() && tokenString.isEmpty()) error("")
        else throw UnauthorizedException("token ${token.uid} unauthorized")
    }

    fun checkToken(tokenString: String): Boolean = redis[parseToken(tokenString).key] == tokenString

    fun throwReGenerateToken(tokenString: String): String {
        val token = parseToken(tokenString)
        val saveTokenString = redis[token.key]
        return if (saveTokenString == tokenString) throwGenerateToken(Token(uid = token.uid, platform = token.platform))
        else if (saveTokenString.isNullOrEmpty() && tokenString.isEmpty()) error("")
        else throw UnauthorizedException("token ${token.uid} unauthorized")
    }

    fun removeToken(tokenString: String) {
        val token = parseToken(tokenString)
        val saveTokenString = redis[token.key]
        if (saveTokenString == tokenString) redis.remove(token.key)
        else if (saveTokenString.isNullOrEmpty() && tokenString.isEmpty()) error("")
        else throw UnauthorizedException("token ${token.uid} unauthorized")
    }

    fun removeAllTokens(uid: Int) {
        if (uid > 0) {
            for (tokenString in Token.keys(uid)) redis.remove(tokenString)
        }
    }
}