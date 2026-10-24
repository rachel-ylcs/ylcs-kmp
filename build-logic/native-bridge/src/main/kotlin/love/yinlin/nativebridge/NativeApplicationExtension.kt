package love.yinlin.nativebridge

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/** Opt-in application packaging; the bridge alone never creates executables or downloads runtimes. */
abstract class NativeApplicationExtension @Inject constructor(objects: ObjectFactory) {
    abstract val enabled: Property<Boolean>
    abstract val entryPoint: Property<String>
    abstract val baseName: Property<String>
    abstract val arguments: ListProperty<String>
    abstract val resources: ConfigurableFileCollection

    val runtimes: NamedDomainObjectContainer<NativeRuntime> = objects.domainObjectContainer(NativeRuntime::class.java) {
        objects.newInstance(NativeRuntime::class.java, it)
    }

    init {
        enabled.convention(false)
        entryPoint.convention("main")
        arguments.convention(emptyList())
    }

    /** Uses the Konan identifier, e.g. mingw_x64, even when the Gradle target is called windows. */
    fun runtime(target: String, configure: Action<NativeRuntime>) {
        configure.execute(runtimes.maybeCreate(target))
    }
}
