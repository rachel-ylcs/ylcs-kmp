package love.yinlin.nativebridge

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.nio.file.Path

/** A separate process owns the Native runtime and its threads until process exit. */
object NativeLibraryExportProbe {
    @JvmStatic
    fun main(arguments: Array<String>) {
        val exported = SymbolLookup.libraryLookup(Path.of(arguments.single()), Arena.global()).find("probe_symbols").orElseThrow()
        val call = Linker.nativeLinker().downcallHandle(exported, FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT))
        println("NATIVE_EXPORTED_RESULT=${call.invokeWithArguments(41)}")
    }
}
