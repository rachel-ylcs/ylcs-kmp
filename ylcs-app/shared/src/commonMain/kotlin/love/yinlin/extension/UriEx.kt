package love.yinlin.extension

import love.yinlin.foundation.Context
import love.yinlin.uri.ImplicitUri
import love.yinlin.uri.Uri

expect fun Uri.toImplicitUri(context: Context): ImplicitUri