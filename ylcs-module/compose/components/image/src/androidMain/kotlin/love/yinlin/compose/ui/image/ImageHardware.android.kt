package love.yinlin.compose.ui.image

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView

@Composable
internal actual fun imageHardwareAllowed(): Boolean {
    val view = LocalView.current
    if (LocalInspectionMode.current || (view.isAttachedToWindow && !view.isHardwareAccelerated)) return false
    var target: View? = view

    while (target != null) {
        if (target.layerType == View.LAYER_TYPE_SOFTWARE) return false
        target = target.parent as? View
    }
    return true
}