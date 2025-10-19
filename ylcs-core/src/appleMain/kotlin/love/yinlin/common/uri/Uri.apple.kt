package love.yinlin.common

import kotlinx.io.files.Path
import love.yinlin.common.uri.Uri
import platform.Foundation.NSURL

fun NSURL.toUri(): Uri = Uri.parse(this.absoluteString ?: "") ?: Uri.Empty
fun Uri.toNSUrl(): NSURL = NSURL.URLWithString(this.toString())!!
fun NSURL.toPath(): Path? = this.path?.let(::Path)