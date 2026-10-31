import groovy.json.JsonSlurper
import love.yinlin.repository.C
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.dokka.gradle.DokkaPlugin

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.dokka,
    )
}

parent!!.subprojects.forEach { p ->
    if (p != project) {
        p.plugins.withType<DokkaPlugin> {
            dependencies.dokka(dependencies.project(mapOf("path" to p.path)))
        }
    }
}

if (providers.gradleProperty("rachel.dokka").orNull == "true") {
    val nativeDirectory = rootProject.file("native-compose")
    val compose = JsonSlurper().parse(nativeDirectory.resolve("upstream/compose.json")) as Map<*, *>
    val modules = compose["modules"] as Map<*, *>
    val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
    dependencies {
        dokka(libs.native.skiko.asProvider())
        dokka(libs.native.skiko.skottie)
        for (module in modules.values) {
            val alias = (module as Map<*, *>)["alias"] as String
            dokka(catalog.findLibrary(alias).get())
        }
    }
}

dokka {
    dokkaPublications.html {
        moduleName = "Rachel"
        moduleVersion = C.versionName
        outputDirectory = C.root.docs.dokka
        failOnWarning = false
    }
}
