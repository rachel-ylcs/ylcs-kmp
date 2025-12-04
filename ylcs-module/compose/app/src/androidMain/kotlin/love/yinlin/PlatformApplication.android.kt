package love.yinlin

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import love.yinlin.extension.BaseLazyReference

@Stable
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: BaseLazyReference<A>,
    delegate: PlatformContextDelegate,
) : Application<A>(self, delegate) {
    @Composable
    open fun BeginContent(activity: ComposeActivity) {}

    open fun onIntent(intent: Intent) {}
}