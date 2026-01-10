package love.yinlin.platform.ffi

import love.yinlin.extension.lazyName
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.SymbolLookup
import java.lang.invoke.MethodHandle
import java.nio.charset.StandardCharsets

abstract class NativeLibrary(private val libName: String? = null) {
    companion object {
        private val linker: Linker by lazy { Linker.nativeLinker() }
    }

    private val lookup: SymbolLookup by lazy {
        if (libName != null) SymbolLookup.libraryLookup(libName, Arena.global())
        else linker.defaultLookup()
    }

    // 函数解析
    fun declare(vararg type: Layout, retType: Layout? = null): FunctionDescriptor =
        if (retType == null) FunctionDescriptor.ofVoid(*type) else FunctionDescriptor.of(retType, *type)

    fun func(vararg type: Layout, retType: Layout? = null) = lazyName { name ->
        requireNotNull(linker.downcallHandle(lookup.find(name).get(), declare(*type, retType = retType))) {
            "can't find function [$name] in $libName."
        }
    }

    fun upcall(handle: MethodHandle, function: FunctionDescriptor): Address = linker.upcallStub(handle, function, Arena.global())

    // 内存分配

    inline fun <R> useMemory(block: (Arena) -> R) = Arena.ofConfined().use(block)
    fun Arena.astr(str: String): Address = allocateFrom(str, StandardCharsets.UTF_8)
    fun Arena.wstr(str: String): Address = allocateFrom(str, StandardCharsets.UTF_16LE)

    val Address.isNull get() = this == Address.NULL
    val Address.isNotNull get() = this != Address.NULL

    val Long.reinterpret: Address get() = Address.ofAddress(this)
    val Address.reinterpret: Long get() = this.address()
}