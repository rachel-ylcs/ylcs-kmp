package love.yinlin.platform.ffi

import java.lang.foreign.Arena
import java.lang.foreign.Linker
import java.lang.foreign.MemoryLayout.PathElement
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodType

class ErrorBinder(handle: MethodHandle) {
    internal companion object {
        private val capturedStateLayout = Linker.Option.captureStateLayout()
        private val errorHandle = capturedStateLayout.varHandle(PathElement.groupElement("GetLastError"))
        internal val errorLinkerOption = Linker.Option.captureCallState("GetLastError")
    }

    private val invokeHandle = handle
        .asSpreader(Array<Any?>::class.java, handle.type().parameterCount() - 1)
        .asType(MethodType.methodType(Any::class.java, Address::class.java, Array<Any?>::class.java))

    private val threadLastError = ThreadLocal<Win32.DWORD>()

    val lastError: Win32.DWORD get() = threadLastError.get() ?: Win32.ERROR_SUCCESS

    operator fun invoke(arena: Arena, vararg args: Any?): Any? {
        val capturedState = arena.allocate(capturedStateLayout)
        val result = invokeHandle.invokeExact(capturedState, args)
        threadLastError.set(errorHandle.get(capturedState, 0L) as Win32.DWORD)
        return result
    }
}
