package love.yinlin.nativebridge

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.file.Directory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderConvertible
import javax.inject.Inject

abstract class NativeBridgeExtension @Inject constructor(objects: ObjectFactory) {
    internal val localSdlSource = objects.directoryProperty()

    /** Prepared SDL source tree in the repository's active Native Compose generation. */
    val sdlSourceDirectory: Provider<Directory> get() = localSdlSource

    val skikoTestRuntimeEnabled = objects.property(Boolean::class.java).convention(false)

    /** Stage Skiko's ICU locale data beside host Native tests, without adding application binaries. */
    fun skikoTests() {
        skikoTestRuntimeEnabled.set(true)
    }

    val redirects: NamedDomainObjectContainer<NativeDependencyRule> = objects.domainObjectContainer(NativeDependencyRule::class.java) {
        objects.newInstance(NativeDependencyRule::class.java, it)
    }

    fun redirect(name: String, configure: Action<NativeDependencyRule>) {
        redirects.create(name, configure)
    }

    /** Returns this plugin's pinned Native coordinates for an owned upstream `group:artifact`. */
    fun port(upstream: String): String {
        val rule = redirects.singleOrNull { it.upstream.get() == upstream }
        require(rule != null && rule.module.isPresent) { "No owned Maven port for $upstream" }
        return rule.module.get()
    }

    fun port(upstream: MinimalExternalModuleDependency): String {
        val notation = "${upstream.module.group}:${upstream.module.name}"
        val rule = redirects.singleOrNull { it.upstream.get() == notation }
        require(rule != null && rule.module.isPresent) { "No owned Maven port for $notation" }
        NativeVersionSelection.validate(upstream, rule.upstreamVersion.get(), rule.name)
        return rule.module.get()
    }

    fun port(upstream: Provider<out MinimalExternalModuleDependency>): Provider<String> = upstream.map { port(it) }

    fun port(upstream: ProviderConvertible<out MinimalExternalModuleDependency>): Provider<String> = port(upstream.asProvider())
}
