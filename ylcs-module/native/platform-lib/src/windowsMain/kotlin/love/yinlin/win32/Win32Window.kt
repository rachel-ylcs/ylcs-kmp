@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.win32

import kotlinx.cinterop.*
import platform.windows.*

// 任务栏

object TaskBar {
    fun show(visible: Boolean) {
        SetWindowPos(FindWindowW("Shell_traywnd", null), HWND_TOPMOST, 0, 0, 0, 0, (if (visible) SWP_SHOWWINDOW else SWP_HIDEWINDOW).convert())
    }

    enum class IconStyle(val value: UInt) {
        Normal(0U), Busy(1U), Info(2U), Error(4U), Warning(8U);
    }

    @Suppress("RETURN_VALUE_NOT_USED")
    fun iconProgress(hwnd: HWND, style: IconStyle, progress: Float) = memScoped {
        if (CoInitialize(null) >= 0) {
            val ppTbl = alloc<CPointerVar<ITaskbarList3>>()
            val hr = CoCreateInstance(CLSID_TaskbarList.ptr, null, CLSCTX_ALL.convert(), IID_ITaskbarList3.ptr, ppTbl.ptr.reinterpret())
            val tbl = ppTbl.value
            if (hr >= 0 && tbl != null) {
                tbl.pointed.lpVtbl?.pointed?.let { vtb ->
                    vtb.SetProgressState?.invoke(tbl, hwnd, style.value.convert())
                    if (style.value != TBPF_NOPROGRESS && style.value != TBPF_INDETERMINATE) {
                        vtb.SetProgressValue?.invoke(tbl, hwnd, (progress.coerceIn(0f, 1f) * 100).toULong(), 100UL)
                    }
                    vtb.Release?.invoke(tbl)
                }
            }
            CoUninitialize()
        }
    }
}