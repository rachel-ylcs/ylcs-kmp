package love.yinlin.compose.ui.node

import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun Modifier.keepSize(): Modifier = this.requiredSizeIn(minWidth = 1.dp, minHeight = 1.dp)