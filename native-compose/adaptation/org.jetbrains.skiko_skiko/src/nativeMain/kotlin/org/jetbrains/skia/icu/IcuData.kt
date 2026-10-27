package org.jetbrains.skia.icu

import org.jetbrains.skia.ExternalSymbolName
import org.jetbrains.skia.impl.InteropPointer
import org.jetbrains.skia.impl.interopScope
import org.jetbrains.skiko.resources.DesktopResourceSupport

/** Registers the complete locale package once; ICU retains its read-only mapping for the process. */
internal object IcuData {
    private val status = interopScope {
        val directory = DesktopResourceSupport.baseDirectory()
        val adjacent = "${directory}icudtl.dat"
        val bundle = "${directory}../MacOS/icudtl.dat"
        val path = when {
            DesktopResourceSupport.isFile(adjacent) -> adjacent
            DesktopResourceSupport.isFile(bundle) -> bundle
            else -> error("ICU locale data is missing beside the Native executable; stage it with the Native bridge plugin")
        }
        _nLoadIcuData(toInterop(path))
    }

    fun ensureLoaded() {
        check(status <= 0) { "Cannot load the packaged ICU locale data: $status" }
    }
}

@ExternalSymbolName("org_jetbrains_skia_icu_IcuData__1nLoad")
private external fun _nLoadIcuData(path: InteropPointer): Int
