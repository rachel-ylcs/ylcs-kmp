package love.yinlin.nativebridge

import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import javax.inject.Inject

/** Files are copied next to the executable. Dependencies may use classifier@extension notation. */
abstract class NativeRuntime @Inject constructor(private val targetName: String) : Named {
    override fun getName(): String = targetName
    abstract val files: ConfigurableFileCollection
    abstract val dependencies: ListProperty<String>
    /** Maven notation to final filename, for example a classifier artifact renamed to icudtl.dat. */
    abstract val artifacts: MapProperty<String, String>

    init {
        dependencies.convention(emptyList())
        artifacts.convention(emptyMap())
    }
}
