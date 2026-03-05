package love.yinlin.uri

import platform.Foundation.NSURL

fun NSURL.toUri(): Uri = Uri.parse(this.absoluteString ?: "") ?: Uri.Empty
fun Uri.toNSUrl(): NSURL? = NSURL.URLWithString(this.toString())