package love.yinlin.tpl.douyin

import androidx.compose.runtime.Stable
import io.ktor.http.encodeURLParameter
import io.ktor.http.formUrlEncode
import love.yinlin.extension.DateEx
import love.yinlin.extension.mapString
import love.yinlin.foundation.cryptography.RC4
import love.yinlin.foundation.cryptography.SM3
import kotlin.io.encoding.Base64
import kotlin.random.Random

@Stable
object DouyinEncoder {
    // 生成指纹

    private const val BASE36_DIGITS = "0123456789abcdefghijklmnopqrstuvwxyz"
    private const val FINGERPRINT_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

    fun buildFingerprint(): String {
        val base36 = buildString {
            var current = DateEx.CurrentLong
            while (current > 0) {
                append(BASE36_DIGITS[(current % 36).toInt()])
                current /= 36
            }
            reverse()
        }
        val chars = CharArray(36) { FINGERPRINT_ALPHABET.random() }
        for (index in [8, 13, 18, 23]) chars[index] = '_'
        chars[14] = '4'
        val oldIndex = FINGERPRINT_ALPHABET.indexOf(chars[19])
        chars[19] = FINGERPRINT_ALPHABET[(oldIndex and 3) or 8]
        return "verify_${base36}_${chars.concatToString()}"
    }

    // 构造Token

    private const val GUEST_ALPHABET =  "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+-"

    fun buildMsToken(): String {
        val arr = CharArray(128) { GUEST_ALPHABET.random() }
        arr[126] = '='
        arr[127] = '='
        return arr.concatToString()
    }

    // a_bogus算法

    private val rc4 = RC4("y")
    private val sm3 = SM3()

    private val PrefixMatrix: Array<IntArray> = [
        [1, 2, 5, 40],
        [1, 0, 0, 0],
        [1, 0, 5, 0],
    ]

    private val UA_DIGEST: IntArray = [
        76, 98, 15, 131, 97, 245, 224, 133,
        122, 199, 241, 166, 79, 34, 90, 191,
        128, 126, 122, 98, 66, 11, 14, 40,
        49, 110, 110, 173, 67, 96, 138, 252
    ]

    private const val CUSTOM_BASE64 = "Dkdpgh2ZmsQB80/MfvV36XI1R45-WUAlEixNLwoqYTOPuzKFjJnry79HbGcaStCe"
    private const val STANDARD_BASE64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    fun aBogus(query: String): String {
        val start = DateEx.CurrentLong
        val end = start + Random.nextInt(4, 9)
        val paramsHash = sm3.encode(sm3.encode(query + "cus"))
        val methodHash = sm3.encode(sm3.encode("GETcus"))

        // 生成浏览器指纹
        val innerWidth = Random.nextInt(1280, 1921)
        val innerHeight = Random.nextInt(720, 1081)
        val outerWidth = Random.nextInt(innerWidth, 1921)
        val outerHeight = Random.nextInt(innerHeight, 1081)
        val fingerprint = [
            innerWidth,
            innerHeight,
            outerWidth,
            outerHeight,
            0,
            if (Random.nextBoolean()) 0 else 30,
            0,
            0,
            outerWidth,
            outerHeight,
            outerWidth,
            outerHeight,
            innerWidth,
            innerHeight,
            24,
            24,
            "Win32",
        ].joinToString("|").encodeToByteArray()

        val data: IntArray = [
            44,
            (end ushr 24).toInt() and 0xFF,
            0,
            0,
            0,
            0,
            24,
            paramsHash[21].toUByte().toInt(),
            methodHash[21].toUByte().toInt(),
            0,
            UA_DIGEST[23].toByte().toUByte().toInt(),
            (end ushr 16).toInt() and 0xFF,
            0,
            0,
            0,
            1,
            0,
            239,
            paramsHash[22].toUByte().toInt(),
            methodHash[22].toUByte().toInt(),
            UA_DIGEST[24].toByte().toUByte().toInt(),
            (end ushr 8).toInt() and 0xFF,
            0,
            0,
            0,
            0,
            (end ushr 0).toInt() and 0xFF,
            0,
            0,
            14,
            (start ushr 24).toInt() and 0xFF,
            (start ushr 16).toInt() and 0xFF,
            0,
            (start ushr 8).toInt() and 0xFF,
            (start ushr 0).toInt() and 0xFF,
            3,
            (end ushr 32).toInt() and 0xFF,
            1,
            (start ushr 32).toInt() and 0xFF,
            1,
            fingerprint.size,
            0,
            0,
            0,
        ]

        var checksum = 0
        for (value in data) checksum = checksum xor value

        val dataBytes = ByteArray(data.size) { index -> data[index].toByte() }

        val randomPrefix = ByteArray(12)
        var pos = 0
        for (mask in PrefixMatrix) {
            val number = Random.nextInt(10000)
            val low = number and 0xFF
            val high = number ushr 8
            randomPrefix[pos++] = ((low and 170) or mask[0]).toByte()
            randomPrefix[pos++] = ((low and 85) or mask[1]).toByte()
            randomPrefix[pos++] = ((high and 170) or mask[2]).toByte()
            randomPrefix[pos++] = ((high and 85) or mask[3]).toByte()
        }

        val encrypted = randomPrefix + rc4.encode(dataBytes + fingerprint + byteArrayOf(checksum.toByte()))

        // 按自定义码表转换
        return Base64.encode(encrypted).mapString { ch ->
            val index = STANDARD_BASE64.indexOf(ch)
            if (index >= 0) CUSTOM_BASE64[index] else ch // '=' padding 不转换
        }
    }

    // 生成Url

    fun buildUrl(id: String, cookie: DouyinCookie): String {
        val query = [
            "device_platform" to "webapp",
            "aid" to "6383",
            "channel" to "channel_pc_web",
            "pc_client_type" to "1",
            "update_version_code" to "170400",
            "version_code" to "170400",
            "version_name" to "17.4.0",
            "cookie_enabled" to "true",
            "screen_width" to "1920",
            "screen_height" to "1080",
            "browser_language" to "zh-CN",
            "browser_platform" to "Win32",
            "browser_name" to "Chrome",
            "browser_version" to "90.0.4430.212",
            "browser_online" to "true",
            "engine_name" to "Blink",
            "engine_version" to "90.0.4430.212",
            "os_name" to "Windows",
            "os_version" to "10",
            "cpu_core_num" to "8",
            "device_memory" to "8",
            "platform" to "PC",
            "support_h265" to "1",
            "support_dash" to "1",
            "sec_user_id" to id,
            "max_cursor" to "0",
            "count" to "18",
            "locate_query" to "false",
            "show_live_replay_strategy" to "1",
            "need_time_list" to "1",
            "time_list_query" to "0",
            "whale_cut_token" to "",
            "cut_version" to "1",
            "publish_video_strategy_type" to "2",
            "verifyFp" to cookie.fp,
            "fp" to cookie.fp,
            "msToken" to cookie.msToken
        ].formUrlEncode()
        return "${DouyinUrl.POST_URL}?${query}&a_bogus=${aBogus(query).encodeURLParameter()}"
    }
}