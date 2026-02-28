package love.yinlin

import androidx.compose.runtime.Stable
import love.yinlin.compose.extension.LazyStateReference

@Stable
internal val mApp = LazyStateReference<AbstractRachelApplication>()

@Stable
val app by mApp

val isAppInitialized: Boolean get() = mApp.isInit