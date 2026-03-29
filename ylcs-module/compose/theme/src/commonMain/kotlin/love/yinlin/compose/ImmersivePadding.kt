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
import androidx.compose.runtime.State
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import love.yinlin.compose.extension.rememberRefState
import love.yinlin.compose.extension.staticLocalComposition

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

    val withoutStart: ImmersivePadding get() = this.copy(start = Dp.Hairline)
    val withoutEnd: ImmersivePadding get() = this.copy(end = Dp.Hairline)
    val withoutTop: ImmersivePadding get() = this.copy(top = Dp.Hairline)
    val withoutBottom: ImmersivePadding get() = this.copy(bottom = Dp.Hairline)
    val withoutHorizontal: ImmersivePadding get() = this.copy(start = Dp.Hairline, end = Dp.Hairline)
    val withoutVertical: ImmersivePadding get() = this.copy(top = Dp.Hairline, bottom = Dp.Hairline)

    companion object {
        val Zero = ImmersivePadding(PaddingValues.Zero)
    }
}

val LocalImmersivePadding = staticLocalComposition { ImmersivePadding.Zero }

@Composable
fun rememberImmersivePadding(): State<ImmersivePadding> {
    val inset = WindowInsets.systemBars.asPaddingValues()
    val padding = rememberRefState { ImmersivePadding(inset) }

    LaunchedEffect(inset, LocalDevice.current) {
        padding.value = ImmersivePadding(inset)
    }

    return padding
}