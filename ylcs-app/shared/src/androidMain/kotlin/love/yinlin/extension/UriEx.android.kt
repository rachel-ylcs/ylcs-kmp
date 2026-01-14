package love.yinlin.extension

import love.yinlin.foundation.Context
import love.yinlin.uri.ContentUri
import love.yinlin.uri.ImplicitUri
import love.yinlin.uri.Uri

actual fun Uri.toImplicitUri(context: Context): ImplicitUri = ContentUri(context.application, this.toString())