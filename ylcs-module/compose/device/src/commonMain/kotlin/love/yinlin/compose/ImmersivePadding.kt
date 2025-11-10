package love.yinlin.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Stable
data class ImmersivePadding(
    val start: Dp,
    val end: Dp,
    val top: Dp,
    val bottom: Dp
) : PaddingValues {
    constructor(padding: PaddingValues) : this(
        start = padding.calculateStartPadding(LayoutDirection.Ltr),
        end = padding.calculateEndPadding(LayoutDirection.Ltr),
        top = padding.calculateTopPadding(),
        bottom = padding.calculateBottomPadding()
    )
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = start
    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = end
    override fun calculateTopPadding(): Dp = top
    override fun calculateBottomPadding(): Dp = bottom

    val withoutStart: ImmersivePadding get() = this.copy(start = 0.dp)
    val withoutEnd: ImmersivePadding get() = this.copy(end = 0.dp)
    val withoutTop: ImmersivePadding get() = this.copy(top = 0.dp)
    val withoutBottom: ImmersivePadding get() = this.copy(bottom = 0.dp)
    val withoutHorizontal: ImmersivePadding get() = this.copy(start = 0.dp, end = 0.dp)
    val withoutVertical: ImmersivePadding get() = this.copy(top = 0.dp, bottom = 0.dp)
}

val LocalImmersivePadding = localComposition<ImmersivePadding>()

@Composable
fun rememberImmersivePadding(): ImmersivePadding {
    val inset = WindowInsets.systemBars.asPaddingValues()
    var padding by rememberRefState { ImmersivePadding(inset) }
    LaunchedEffect(inset, LocalDevice.current) {
        padding = ImmersivePadding(inset)
    }
    return padding
}