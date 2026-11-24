package love.yinlin.platform.ffi

import love.yinlin.extension.lazyName
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
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

    protected fun lookup(name: String): MemorySegment = lookup.find(name).orElse(null) ?: throw NoSuchElementException("Could not find '$name' in ${libName ?: "Default"}")

    protected fun downcall(address: MemorySegment, descriptor: FunctionDescriptor, vararg options: Linker.Option): MethodHandle = requireNotNull(linker.downcallHandle(address, descriptor, *options))

    fun declare(vararg type: Layout, retType: Layout? = null): FunctionDescriptor = if (retType == null) FunctionDescriptor.ofVoid(*type) else FunctionDescriptor.of(retType, *type)

    fun func(descriptor: NativeLibrary.() -> FunctionDescriptor) = lazyName { name ->
        downcall(lookup(name), descriptor())
    }

    fun func(vararg options: Linker.Option, symbol: String? = null, descriptor: NativeLibrary.() -> FunctionDescriptor) = lazyName { name ->
        downcall(lookup(symbol ?: name), descriptor(), *options)
    }

    fun upcall(handle: MethodHandle, function: FunctionDescriptor): Address = linker.upcallStub(handle, function, Arena.global())
}
