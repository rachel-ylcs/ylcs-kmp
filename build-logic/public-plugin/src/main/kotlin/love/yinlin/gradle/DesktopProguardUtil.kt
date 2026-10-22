package love.yinlin.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

private const val ProguardUsage = "rachel-desktop-proguard"
private const val ProguardElements = "rachelDesktopProguardElements"

internal fun Project.registerDesktopProguard(compilation: KotlinCompilation<*>) {
    val projectDirectory = layout.projectDirectory

    val rules = files({
        compilation.allKotlinSourceSets.map { projectDirectory.dir("src/${it.name}/resources/META-INF/proguard") }
    }).asFileTree.matching {
        include("*.pro")
    }

    configurations.create(ProguardElements) {
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>(ProguardUsage))
        outgoing.artifacts(rules.elements)
    }
}

@Suppress("UnstableApiUsage")
internal fun Project.desktopProguardFiles(compilation: KotlinCompilation<*>): FileCollection {
    val runtime = configurations.getByName(requireNotNull(compilation.runtimeDependencyConfigurationName))
    val dependencies = runtime.incoming.artifactView {
        isLenient = true
        componentFilter { it is ProjectComponentIdentifier }
        withVariantReselection()
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>(ProguardUsage))
    }.files
    val ownRules = configurations.getByName(ProguardElements).artifacts.files

    val ownConventionalRules = files(desktopProguardKMPDir, desktopProguardJVMDir).asFileTree.matching {
        include("*.pro")
    }

    return files(dependencies, ownRules, ownConventionalRules)
}
