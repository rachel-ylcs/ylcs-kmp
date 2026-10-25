package love.yinlin.nativebridge

import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

/** One upstream module/version mapped only for the listed Konan target identifiers. */
abstract class NativeDependencyRule @Inject constructor(private val ruleName: String) : Named {
    override fun getName(): String = ruleName

    abstract val upstream: Property<String>
    abstract val upstreamVersion: Property<String>
    abstract val targets: SetProperty<String>
    /** Targets supplied by the replacement's shared metadata; concrete target routing still uses [targets]. */
    abstract val metadataTargets: SetProperty<String>
    abstract val module: Property<String>

    /** Same-build project alternative. Its Maven coordinates must be configured by its producer. */
    abstract val projectPath: Property<String>

    init {
        metadataTargets.convention(targets)
    }

    internal fun validate() {
        require(upstream.get().split(':').size == 2) { "$name: upstream must be group:artifact" }
        require(upstreamVersion.get().isNotBlank()) { "$name: a tested upstream version is required" }
        require(targets.get().isNotEmpty()) { "$name: at least one Konan target is required" }
        require(metadataTargets.get().containsAll(targets.get())) { "$name: metadata must cover every redirected target" }
        require(module.isPresent.xor(projectPath.isPresent)) { "$name: choose module OR projectPath" }
        if (module.isPresent) require(module.get().split(':').size == 3) { "$name: module must be group:artifact:version" }
    }
}
