package love.yinlin.vendor

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.MessageCollectorAccess
import java.nio.file.InvalidPathException
import java.nio.file.Path

@OptIn(ExperimentalCompilerApi::class)
class VendorDiagnosticsRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true
    override val pluginId: String = "love.yinlin.native-vendor-diagnostics"

    @OptIn(MessageCollectorAccess::class)
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val generation = configuration.get(VendorDiagnosticsOptions.generation) ?: return
        val collector = configuration.get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY) ?: return
        // Metadata and Native compilations mix upstream and adaptation files.
        configuration.put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, object : MessageCollector by collector {
            override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
                if (severity.isWarning && location != null && isInGeneration(location.path, generation)) return
                collector.report(severity, message, location)
            }
        })
    }

    private fun isInGeneration(source: String, generation: Path): Boolean {
        val path = try { Path.of(source) } catch (_: InvalidPathException) { return false }
        return path.isAbsolute && path.normalize().startsWith(generation)
    }
}
