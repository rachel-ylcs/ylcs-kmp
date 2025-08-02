package love.yinlin.common

fun android.net.Uri.toUri(): Uri = Uri.parse(this.toString()) ?: Uri.Empty
fun Uri.toAndroidUri(): android.net.Uri = android.net.Uri.parse(this.toString())