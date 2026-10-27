package love.yinlin.nativebridge

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

internal object NativeRuntimeAttributes {
    val artifactSuffixes = mapOf("mingw_x64" to "mingwx64", "linux_x64" to "linuxx64", "macos_arm64" to "macosarm64")

    fun configure(project: Project, configuration: Configuration, target: String) {
        configuration.attributes {
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
            attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, "kotlin-api"))
            attribute(Attribute.of("org.jetbrains.kotlin.native.target", String::class.java), target)
        }
    }
}
