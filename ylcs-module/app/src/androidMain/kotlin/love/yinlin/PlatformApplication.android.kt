package love.yinlin

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import love.yinlin.extension.Reference

@Stable
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: Reference<A?>,
    delegate: PlatformContextDelegate,
) : Application<A>(self, delegate) {
    @Composable
    open fun BeginContent(activity: ComposeActivity) {}

    open fun onActivityCreate(activity: ComposeActivity) {}
    open fun onActivityDestroy(activity: ComposeActivity) {}
    open fun onIntent(intent: Intent) {}

    @Composable
    fun FloatingWindow() {

    }
}