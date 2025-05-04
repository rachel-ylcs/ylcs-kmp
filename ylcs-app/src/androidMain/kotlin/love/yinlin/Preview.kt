package love.yinlin

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import love.yinlin.common.Device
import love.yinlin.common.LocalDevice
import love.yinlin.common.RachelTheme
import love.yinlin.ui.component.layout.EmptyBox


@Composable
fun Content(maxWidth: Dp, maxHeight: Dp) {
    CompositionLocalProvider(LocalDevice provides Device(maxWidth, maxHeight)) {
        RachelTheme(
            darkMode = false,
            device = LocalDevice.current
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.size(500.dp)) {
                    EmptyBox()
                }
            }
        }
    }
}

@Preview(device = "spec:width=360dp,height=800dp,dpi=300")
@Composable fun Preview1() = Content(360.dp, 800.dp)

@Preview(device = "spec:width=800dp,height=600dp,dpi=240")
@Composable fun Preview2() = Content(800.dp, 600.dp)

@Preview(device = Devices.DESKTOP)
@Composable fun Preview3() = Content(1920.dp, 1080.dp)