package love.yinlin.common

fun java.net.URI.toUri(): Uri = Uri.parse(this.toString()) ?: Uri.Empty
fun Uri.toJvmUri(): java.net.URI = java.net.URI.create(this.toString())