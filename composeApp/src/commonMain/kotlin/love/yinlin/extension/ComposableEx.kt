package love.yinlin.extension

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
fun Modifier.shadowStart(value: Dp = 3.dp): Modifier = this.padding(start = value).shadow(value)

@Stable
fun Modifier.shadowEnd(value: Dp = 3.dp): Modifier = this.padding(end = value).shadow(value)

@Stable
fun Modifier.shadowTop(value: Dp = 3.dp): Modifier = this.padding(top = value).shadow(value)

@Stable
fun Modifier.shadowBottom(value: Dp = 3.dp): Modifier = this.padding(bottom = value).shadow(value)