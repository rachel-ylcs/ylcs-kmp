package love.yinlin.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType

class RachelPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        if (target.extensions.findByType<BuildDefaults>() == null) {
            target.extensions.add("rachelDefaults", BuildDefaults())
        }
        target.extensions.add("rachel", RachelExtension(target))
    }
}
