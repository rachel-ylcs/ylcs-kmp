package love.yinlin.tpl.bilibili

import androidx.compose.runtime.Stable
import love.yinlin.extension.DateEx
import love.yinlin.extension.makeObject
import love.yinlin.extension.toJsonString
import love.yinlin.foundation.cryptography.MD5

@Stable
object BilibiliEncoder {
    // Wbi摘要

    private val TABLE: IntArray = [
        46,47,18,2,53,8,23,32,15,50,10,31,58,3,45,35,27,43,5,49,
        33,9,42,19,29,28,14,39,12,38,41,13,37,48,7,16,24,55,40,
        61,26,17,0,1,60,51,30,4,22,25,54,21,56,59,6,63,57,62,11,36,20,34,44,52,
    ]

    internal fun percentEncode(value: String): String = buildString {
        val hex = "0123456789ABCDEF"
        for (byte in value.encodeToByteArray()) {
            val number = byte.toInt() and 0xff
            if (number in 0x41..0x5a || number in 0x61..0x7a || number in 0x30..0x39 ||
                number == 0x2d || number == 0x2e || number == 0x5f || number == 0x7e) {
                append(number.toChar())
            } else {
                append('%')
                append(hex[number ushr 4])
                append(hex[number and 0xf])
            }
        }
    }

    internal fun signWbi(params: Map<String, String>, visitor: BilibiliVisitor): String {
        val timestamp = (DateEx.CurrentLong + visitor.serverTimeOffset) / 1000L
        val keys = visitor.imgKey + visitor.subKey
        val mixinKey = buildString(32) {
            for (index in TABLE.take(32)) append(keys[index])
        }
        val values = LinkedHashMap<String, String>().apply {
            putAll(params)
            put("wts", timestamp.toString())
        }
        val query = values.entries.sortedBy { it.key }.joinToString("&") { (key, value) ->
            val filtered = value.filter { it !in "!'()*" }
            "${percentEncode(key)}=${percentEncode(filtered)}"
        }
        return "$query&w_rid=${MD5.Default.encodeToHex(query + mixinKey)}"
    }

    internal fun sign(params: Map<String, String>) : String = params.entries.sortedBy { it.key }.joinToString("&") { (key, value) ->
        val filtered = value.filter { it !in "!'()*" }
        "${percentEncode(key)}=${percentEncode(filtered)}"
    }

    // 浏览器指纹
    internal val DefaultFingerprint: Array<Pair<String, String>> = [
        "dm_img_list" to "[]",
        "dm_img_str" to "V2ViR0wgMS4wIChPcGVuR0wgRVMgMi4wIENocm9taXVtKQ",
        "dm_cover_img_str" to "QU5HTEUgKE5WSURJQSwgTlZJRElBIEdlRm9yY2UgUlRYIDQwNjAgKDB4MDAwMDI4ODIpIERpcmVjdDNEMTEgdnNfNV8wIHBzXzVfMCwgRDNEMTEpR29vZ2xlIEluYy4gKE5WSURJQS",
        "dm_img_inter" to makeObject {
            arr("ds") { }
            arr("wh") {
                add(4181)
                add(3287)
                add(61)
            }
            arr("of") {
                add(168)
                add(336)
                add(168)
            }
        }.toJsonString()
    ]

    // 设备信息
    internal val DefaultDeviceInfo: Array<Pair<String, String>> = [
        "platform" to "web",
        "web_location" to "1550101",
        "x-bili-locale-json" to makeObject {
            obj("c_locale") {
                "language" with "zh"
                "script" with "Hans"
            }
            "always_translate" with false
        }.toJsonString(),
        "x-bili-device-req-json" to makeObject {
            "platform" with "web"
            "device" with "pc"
            "spmid" with "333.1387"
            "mobi_app" with "web_cn"
        }.toJsonString(),
    ]
}