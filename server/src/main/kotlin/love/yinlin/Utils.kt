package love.yinlin

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.sql.Timestamp

val String.md5: String get() = try {
	val md = MessageDigest.getInstance("MD5")
	md.update(this.toByteArray(StandardCharsets.UTF_8))
	val hexString = StringBuilder()
	for (b in md.digest()) {
		val hex = Integer.toHexString(0xff and b.toInt())
		if (hex.length == 1) hexString.append('0')
		hexString.append(hex)
	}
	hexString.toString()
}
catch (_: Exception) { "" }

val currentTS: Timestamp get() = Timestamp(System.currentTimeMillis())

fun currentUniqueId(id: Any? = null) = "${System.currentTimeMillis()}${id}"