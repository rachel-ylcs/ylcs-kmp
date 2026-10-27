package love.yinlin.nativebridge

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class NativeBridgeSettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        settings.extensions.add("nativeBridgeSources", NativeBridgeSourcesExtension(settings))
    }
}
