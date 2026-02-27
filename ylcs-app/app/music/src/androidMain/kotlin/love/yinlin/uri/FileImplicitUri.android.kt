package love.yinlin.uri

import love.yinlin.foundation.Context

actual fun Uri.asFileImplicitUri(context: Context): ImplicitUri = ContentUri(context.application, this.toString())