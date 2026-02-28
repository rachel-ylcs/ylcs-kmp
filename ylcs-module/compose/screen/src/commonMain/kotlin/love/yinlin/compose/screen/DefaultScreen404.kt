package love.yinlin.compose.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.text.Text

@Composable
fun DefaultScreen404(manager: ScreenManager) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.padding.v6, Alignment.CenterVertically)
    ) {
        Text(text = Theme.value.noContent404Text, style = Theme.typography.v1.bold)
        PrimaryButton(text = Theme.value.backText, icon = Icons.ArrowBack, style = Theme.typography.v6.bold, onClick = { manager.pop() })
    }
}