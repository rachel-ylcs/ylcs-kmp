@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package love.yinlin

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import love.yinlin.native_compose.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

/** Uses generated accessors and the same deployable files that an ordinary application consumes. */
@Composable
internal fun ResourcesPreview(count: Int) {
    var content by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        content = withContext(Dispatchers.Default) { Res.readBytes("files/目录 含空格.txt").decodeToString() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicText(stringResource(Res.string.resources_title), style = TextStyle(color = Color(0xFF146C69), fontSize = 22.sp))
        BasicText(pluralStringResource(Res.plurals.resource_count, count, count))
        BasicText(stringArrayResource(Res.array.resource_features).joinToString(" · "))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Image(painterResource(Res.drawable.native_vector), contentDescription = "XML vector", modifier = Modifier.size(64.dp))
            Image(painterResource(Res.drawable.native_svg), contentDescription = "SVG", modifier = Modifier.size(64.dp))
            Image(painterResource(Res.drawable.native_tile), contentDescription = "PNG", modifier = Modifier.size(64.dp))
        }
        BasicText(content)
    }
}
