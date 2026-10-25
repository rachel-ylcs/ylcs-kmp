package org.jetbrains.skia

import org.jetbrains.skia.impl.InteropPointer
import org.jetbrains.skia.impl.NativePointer
import org.jetbrains.skia.impl.Stats
import org.jetbrains.skia.impl.interopScope
import org.jetbrains.skia.impl.reachabilityBarrier

actual fun ColorFilter.colorMatrixOrNull(): ColorMatrix? {
    Stats.onNativeCall()
    return try {
        interopScope {
            val values = FloatArray(20)
            val result = toInteropForResult(values)
            if (readNativeColorMatrix(_ptr, result)) {
                result.fromInterop(values)
                ColorMatrix(values)
            }
            else null
        }
    }
    finally {
        reachabilityBarrier(this)
    }
}

@SymbolName("org_jetbrains_skia_ColorFilter__1nGetColorMatrix")
private external fun readNativeColorMatrix(pointer: NativePointer, matrix: InteropPointer): Boolean
