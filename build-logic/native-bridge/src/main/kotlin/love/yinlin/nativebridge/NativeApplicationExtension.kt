package love.yinlin.nativebridge

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class NativeApplicationExtension @Inject constructor(objects: ObjectFactory) {
    abstract val enabled: Property<Boolean>
    abstract val entryPoint: Property<String>
    abstract val baseName: Property<String>
    abstract val arguments: ListProperty<String>
    /** 仅供 Gradle 启动进程使用，不改变二进制及资源的分发位置。 */
    abstract val workingDirectory: DirectoryProperty
    abstract val resources: ConfigurableFileCollection
    val desktop = NativeDesktopDistribution()
    val skikoRuntimeEnabled: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
    val skikoWindowEnabled: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

    val runtimes: NamedDomainObjectContainer<NativeRuntime> = objects.domainObjectContainer(NativeRuntime::class.java) {
        objects.newInstance(NativeRuntime::class.java, it)
    }

    init {
        enabled.convention(false)
        entryPoint.convention("main")
        arguments.convention(emptyList())
    }

    fun runtime(target: String, configure: Action<NativeRuntime>) {
        configure.execute(runtimes.maybeCreate(target))
    }

    fun desktop(configure: Action<NativeDesktopDistribution>) {
        desktop.enabled = true
        configure.execute(desktop)
    }

    fun skikoRuntime() { skikoRuntimeEnabled.set(true) }

    fun skikoWindow() {
        enabled.set(true)
        skikoRuntime()
        skikoWindowEnabled.set(true)
    }
}
