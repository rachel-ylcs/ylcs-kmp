package love.yinlin.nativebridge

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import javax.inject.Inject

abstract class NativeApplicationExtension @Inject constructor(objects: ObjectFactory) {
    abstract val enabled: Property<Boolean>
    abstract val entryPoint: Property<String>
    abstract val baseName: Property<String>
    abstract val arguments: ListProperty<String>
    abstract val workingDirectory: DirectoryProperty
    abstract val resources: ConfigurableFileCollection
    val desktop = NativeDesktopDistribution()
    internal var releaseArchive: Provider<RegularFile>? = null
    val hostReleaseArchive: Provider<RegularFile>
        get() = checkNotNull(releaseArchive) { "No Release application archive was configured for this host" }
    internal val hostRuns = mutableMapOf<NativeBuildType, MutableList<TaskProvider<RunNativeApplication>>>()

    fun findHostRunTask(buildType: NativeBuildType): TaskProvider<RunNativeApplication>? =
        hostRuns[buildType]?.singleOrNull()
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
