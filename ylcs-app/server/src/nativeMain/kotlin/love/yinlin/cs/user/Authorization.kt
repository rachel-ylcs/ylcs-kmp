package love.yinlin.cs.user

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.operations.Cipher
import love.yinlin.cs.ServerLogger
import love.yinlin.cs.UnauthorizedException
import love.yinlin.cs.service.RedisService
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.days

@OptIn(DelicateCryptographyApi::class)
class Authorization(
    private val logger: ServerLogger,
    private val redis: RedisService
) {
    companion object {
        const val TOKEN_SECRET_KEY = "TokenSecretKey"
    }

    private val aesEcb = CryptographyProvider.Default.get(AES.ECB)
    private lateinit var cipher: Cipher

    suspend fun init() {
        // 查询是否保存密钥
        val keyString = redis[TOKEN_SECRET_KEY]
        val key = if (keyString != null) {
            // 读取密钥
            Base64.decode(keyString)
        }
        else {
            // 创建密钥
            val newKey = aesEcb.keyGenerator().generateKey()
            val bytes = newKey.encodeToByteArray(AES.Key.Format.RAW)
            redis[TOKEN_SECRET_KEY] = Base64.encode(bytes)
            bytes
        }

        val aesKey = aesEcb.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, key)
        cipher = aesKey.cipher()
    }

    suspend fun throwGenerateToken(token: Token): String {
        val encryptedBytes = cipher.encrypt(token.bytes)
        val tokenString = Base64.encode(encryptedBytes)
        redis.setex(token.key, tokenString, 30.days)
        return tokenString
    }

    private suspend fun parseToken(tokenString: String): Token {
        val encryptedBytes = Base64.decode(tokenString)
        val bytes = cipher.decrypt(encryptedBytes)
        return Token.fromBytes(bytes)!!
    }

    suspend fun throwExpireToken(tokenString: String): Int {
        val token = parseToken(tokenString)
        // keyToken 可能是 null 或 已经失效的 token
        val saveTokenString = redis[token.key]
        return if (saveTokenString == tokenString) token.uid
        else if (saveTokenString.isNullOrEmpty() && tokenString.isEmpty()) error("")
        else throw UnauthorizedException("token ${token.uid} unauthorized")
    }

    suspend fun checkToken(tokenString: String): Boolean = redis[parseToken(tokenString).key] == tokenString

    suspend fun throwReGenerateToken(tokenString: String): String {
        val token = parseToken(tokenString)
        val saveTokenString = redis[token.key]
        return if (saveTokenString == tokenString) throwGenerateToken(Token(uid = token.uid, platform = token.platform))
        else if (saveTokenString.isNullOrEmpty() && tokenString.isEmpty()) error("")
        else throw UnauthorizedException("token ${token.uid} unauthorized")
    }

    suspend fun removeToken(tokenString: String) {
        val token = parseToken(tokenString)
        val saveTokenString = redis[token.key]
        if (saveTokenString == tokenString) redis.remove(token.key)
        else if (saveTokenString.isNullOrEmpty() && tokenString.isEmpty()) error("")
        else throw UnauthorizedException("token ${token.uid} unauthorized")
    }

    suspend fun removeAllTokens(uid: Int) {
        if (uid > 0) {
            for (tokenString in Token.keys(uid)) redis.remove(tokenString)
        }
    }
}