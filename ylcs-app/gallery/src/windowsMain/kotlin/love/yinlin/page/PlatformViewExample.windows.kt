@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package love.yinlin.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.rawValue
import love.yinlin.compose.ui.PlatformViewEnvironment
import love.yinlin.compose.ui.PlatformViewHost
import love.yinlin.compose.ui.PlatformViewState
import love.yinlin.compose.ui.rememberPlatformViewState
import platform.windows.*

internal actual val platformViewName: String? = "Win32 EDIT"

@Composable
internal actual fun PlatformViewExample(text: String, modifier: Modifier) {
    val state = rememberPlatformViewState { GalleryTextViewState() }
    PlatformViewHost(state, text, modifier)
}

@Stable
private class GalleryTextViewState : PlatformViewState<CPointer<HWND__>, String>() {
    override fun factory(environment: PlatformViewEnvironment): CPointer<HWND__> {
        val view = checkNotNull(CreateWindowExW(
            WS_EX_CLIENTEDGE.toUInt(), "EDIT", "",
            (WS_CHILD or WS_VISIBLE or WS_TABSTOP or WS_VSCROLL or ES_MULTILINE or ES_AUTOVSCROLL or ES_WANTRETURN).toUInt(),
            0, 0, 0, 0, environment.parentWindow, null, GetModuleHandleW(null), null,
        )) { "Cannot create the native text field: ${GetLastError()}" }
        GetStockObject(DEFAULT_GUI_FONT)?.let { SendMessageW(view, WM_SETFONT.toUInt(), it.rawValue.toLong().toULong(), 0L) }
        return view
    }

    override fun synchronize(view: CPointer<HWND__>, previous: String?, next: String) {
        if (previous != next) check(SetWindowTextW(view, next.replace("\n", "\r\n")) != 0) { "Cannot update the native text field: ${GetLastError()}" }
    }

    override fun detach(view: CPointer<HWND__>) {}
}
