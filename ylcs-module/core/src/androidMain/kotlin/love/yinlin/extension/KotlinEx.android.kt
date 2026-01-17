package love.yinlin.extension

import android.os.Build
import androidx.annotation.RequiresApi
import java.lang.ref.Cleaner

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object NativeDestructor {
    val cleaner: Cleaner = Cleaner.create()
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class RAII(constructor: () -> Long, private val destructor: (Long) -> Unit) : Runnable {
    internal val handle: Long = constructor()
    override fun run() = destructor(handle)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
abstract class Destructible(raii: RAII) : AutoCloseable {
    protected var nativeHandle: Long = raii.handle
        private set
    protected open fun onClose() { }

    init {
        NativeDestructor.cleaner.register(this, raii)
    }

    final override fun close() {
        onClose()
        nativeHandle = 0L
    }
}