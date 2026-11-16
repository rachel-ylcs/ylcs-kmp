package love.yinlin.vendor

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import java.nio.file.Path

@OptIn(ExperimentalCompilerApi::class)
class VendorDiagnosticsOptions : CommandLineProcessor {
    override val pluginId: String = "love.yinlin.native-vendor-diagnostics"
    override val pluginOptions: Collection<AbstractCliOption> = listOf(CliOption("generation", "<path>", "Prepared vendor generation directory", required = true))

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        check(option.optionName == "generation")
        configuration.put(generation, Path.of(value).toAbsolutePath().normalize())
    }

    companion object {
        internal val generation = CompilerConfigurationKey.create<Path>("Native vendor generation")
    }
}
