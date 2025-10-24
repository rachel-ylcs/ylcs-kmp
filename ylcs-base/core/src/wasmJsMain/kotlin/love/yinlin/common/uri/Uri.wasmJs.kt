package love.yinlin.common.uri

import org.w3c.dom.url.URL

fun URL.toUri(): Uri = Uri.parse(this.toString()) ?: Uri.Empty
fun Uri.toJsUri(): URL = URL(this.toString())