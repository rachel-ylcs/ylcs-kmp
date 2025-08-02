package love.yinlin.common

fun org.w3c.dom.url.URL.toUri(): Uri = Uri.parse(this.toString()) ?: Uri.Empty
fun Uri.toJsUri(): org.w3c.dom.url.URL = org.w3c.dom.url.URL(this.toString())