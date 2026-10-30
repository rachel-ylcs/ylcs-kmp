package love.yinlin.gradle

import org.gradle.api.file.Directory
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMImportExtension

class IosInterop internal constructor() {
    var discoverClangModulesImplicitly: Boolean = true
    private val packages = mutableListOf<SwiftPMImportExtension.(Directory) -> Unit>()

    /** 从 src/iosMain/spm/<name> 导入本地 SwiftPM 包。 */
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    fun spm(name: String, importedClangModules: List<String> = emptyList()) {
        packages += { directory ->
            localSwiftPackage(
                directory = directory.dir(name),
                products = listOf(product(name, platforms = setOf(iOS()), importedClangModules = importedClangModules.toSet())),
            )
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    fun spm(url: String, version: String, vararg products: String) {
        packages += {
            swiftPackage(
                url = this.url(url),
                version = from(version),
                products = products.map { product(it, platforms = setOf(iOS())) },
            )
        }
    }

    internal fun configure(extension: SwiftPMImportExtension, directory: Directory) {
        extension.discoverClangModulesImplicitly.set(discoverClangModulesImplicitly)
        for (pkg in packages) extension.pkg(directory)
    }
}
