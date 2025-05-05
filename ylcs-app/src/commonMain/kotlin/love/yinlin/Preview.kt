package love.yinlin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.yinlin.common.Device
import love.yinlin.common.LocalDarkMode
import love.yinlin.common.LocalDevice
import love.yinlin.common.RachelTheme
import love.yinlin.ui.component.common.UserLabel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun PreviewLayout() {
    if (!Local.Client.DEVELOPMENT) return
    val phone = Device(360.dp, 800.dp)
    val tablet = Device(600.dp, 800.dp)
    val desktop = Device(1200.dp, 600.dp)

    CompositionLocalProvider(LocalDarkMode provides false) {
        Row(modifier = Modifier.size(360.dp, 800.dp)) {
            CompositionLocalProvider(LocalDevice provides phone) {
                RachelTheme {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        UserLabel("", 1)
                        UserLabel("", 5)
                        UserLabel("", 9)
                        UserLabel("", 13)
                        UserLabel("", 17)
                        UserLabel("", 21)
                        UserLabel("正版焦骨", 1)
                    }
                }
            }
            CompositionLocalProvider(LocalDevice provides tablet) {
                RachelTheme {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        UserLabel("", 1)
                        UserLabel("", 5)
                        UserLabel("", 9)
                        UserLabel("", 13)
                        CompositionLocalProvider(LocalDarkMode provides true) {
                            UserLabel("", 17)
                        }
                        UserLabel("", 21)
                        UserLabel("正版焦骨", 1)
                    }
                }
            }
            CompositionLocalProvider(LocalDevice provides desktop) {
                RachelTheme {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        UserLabel("", 1)
                        UserLabel("", 5)
                        UserLabel("", 9)
                        UserLabel("", 13)
                        UserLabel("", 17)
                        UserLabel("", 21)
                        UserLabel("正版焦骨", 1)
                    }
                }
            }
        }
    }
}