package love.yinlin.common.uri

import java.net.URI

fun URI.toUri(): Uri = Uri.parse(this.toString()) ?: Uri.Empty
fun Uri.toJvmUri(): URI = URI.create(this.toString())