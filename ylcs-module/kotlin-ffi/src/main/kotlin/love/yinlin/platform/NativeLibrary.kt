package love.yinlin.platform

import love.yinlin.extension.lazyName
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.SymbolLookup
import java.lang.invoke.MethodHandle

abstract class NativeLibrary(private val libName: String? = null) {
    companion object {
        private val linker: Linker by lazy { Linker.nativeLinker() }
    }

    private val lookup: SymbolLookup by lazy {
        if (libName != null) SymbolLookup.libraryLookup(libName, Arena.global())
        else linker.defaultLookup()
    }

    fun declare(vararg type: Native.Type, retType: Native.Type? = null): FunctionDescriptor =
        if (retType == null) FunctionDescriptor.ofVoid(*type) else FunctionDescriptor.of(retType, *type)

    fun func(vararg type: Native.Type, retType: Native.Type? = null) = lazyName { name ->
        requireNotNull(linker.downcallHandle(lookup.find(name).get(), declare(*type, retType = retType))) {
            "can't find function [$name] in $libName."
        }
    }

    fun upcall(handle: MethodHandle, function: FunctionDescriptor): Native.Pointer = linker.upcallStub(handle, function, Arena.global())

    fun <R> useMemory(block: (Arena) -> R) = Arena.ofConfined().use(block)
}