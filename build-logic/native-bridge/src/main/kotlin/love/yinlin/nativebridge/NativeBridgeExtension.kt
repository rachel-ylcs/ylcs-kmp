package love.yinlin.nativebridge

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class NativeBridgeExtension @Inject constructor(objects: ObjectFactory) {
    val skikoTestRuntimeEnabled = objects.property(Boolean::class.java).convention(false)

    /** Stage Skiko's Windows ICU data beside Native test executables, without adding application binaries. */
    fun skikoTests() {
        skikoTestRuntimeEnabled.set(true)
    }

    val redirects: NamedDomainObjectContainer<NativeDependencyRule> = objects.domainObjectContainer(NativeDependencyRule::class.java) {
        objects.newInstance(NativeDependencyRule::class.java, it)
    }

    fun redirect(name: String, configure: Action<NativeDependencyRule>) {
        redirects.create(name, configure)
    }
}
