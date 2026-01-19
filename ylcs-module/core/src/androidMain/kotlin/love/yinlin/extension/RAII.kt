package love.yinlin.extension

import android.os.Build
import androidx.annotation.RequiresApi
import java.lang.ref.Cleaner

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class RAII(constructor: () -> Long, private val destructor: (Long) -> Unit) : Runnable {
    internal companion object {
        internal val cleaner: Cleaner = Cleaner.create()
    }

    internal val handle: Long = constructor()
    override fun run() = destructor(handle)
}