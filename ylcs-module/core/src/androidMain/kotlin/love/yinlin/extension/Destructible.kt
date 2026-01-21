package love.yinlin.extension

import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
abstract class Destructible(raii: RAII) {
    protected val nativeHandle: Long = raii.handle
    init {
        RAII.cleaner.register(this, raii)
    }
}