package love.yinlin.foundation

import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.reflect.metaClassName

object StartupID {
    @OptIn(CompatibleRachelApi::class)
    inline operator fun <reified S : Startup> invoke(): String = metaClassName<S>()
}