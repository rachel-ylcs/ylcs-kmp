package love.yinlin.common

fun platform.Foundation.NSURL.toUri(): Uri = Uri.parse(this.absoluteString ?: "") ?: Uri.Empty
fun Uri.toNSUri(): platform.Foundation.NSURL = platform.Foundation.NSURL.URLWithString(this.toString())!!