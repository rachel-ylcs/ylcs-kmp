package love.yinlin.common

import kotlinx.io.files.Path

fun platform.Foundation.NSURL.toUri(): Uri = Uri.parse(this.absoluteString ?: "") ?: Uri.Empty
fun Uri.toNSUrl(): platform.Foundation.NSURL = platform.Foundation.NSURL.URLWithString(this.toString())!!
fun platform.Foundation.NSURL.toPath(): Path? = this.path?.let(::Path)