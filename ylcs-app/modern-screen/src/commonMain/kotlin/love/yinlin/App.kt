package love.yinlin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.compose.ui.text.Text

@Composable
fun App() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "Hello World")
    }
}