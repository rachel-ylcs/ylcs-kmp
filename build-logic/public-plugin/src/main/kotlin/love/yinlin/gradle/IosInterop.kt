package love.yinlin.gradle

import org.gradle.api.file.Directory
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMImportExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultCInteropSettings
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

class IosInterop internal constructor() {
    var discoverClangModulesImplicitly: Boolean = true
    private val packages = mutableListOf<SwiftPMImportExtension.(Directory) -> Unit>()
    private val interops = NativeInterop()
    internal val sources = linkedSetOf<String>()

    /**
     * 注册 iOS CInterop，默认读取 `src/nativeInterop/cinterop/<name>.def`。
     * 静态库由 `src/iosMain/cpp` 构建，可通过 [sources] 传入额外的 `RACHEL_CPP_SOURCE_DIRS`。
     */
    fun cpp(name: String, vararg sources: String, block: DefaultCInteropSettings.() -> Unit = {}) {
        interops.cpp(name, block)
        this.sources += sources
    }

    internal fun configureCpp(target: KotlinNativeTarget) = interops.configure(target)

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
