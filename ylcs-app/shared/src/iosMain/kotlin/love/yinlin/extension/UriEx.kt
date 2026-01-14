package love.yinlin.extension

import love.yinlin.foundation.Context
import love.yinlin.uri.ImplicitUri
import love.yinlin.uri.SandboxUri
import love.yinlin.uri.Uri
import love.yinlin.uri.toNSUrl

actual fun Uri.toImplicitUri(context: Context): ImplicitUri = SandboxUri(this.toNSUrl()!!)