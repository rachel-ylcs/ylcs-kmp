import groovy.json.JsonSlurper
import love.yinlin.repository.C
import org.jetbrains.dokka.gradle.DokkaPlugin

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.dokka,
        libs.plugins.nativeBridge,
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
    dependencies {
        dokka(nativeBridge.port(libs.skiko))
        dokka(nativeBridge.port(libs.skiko.lottie))
        for ((artifact, value) in modules) {
            val module = value as Map<*, *>
            val upstream = module["upstreamGroup"] ?: "org.jetbrains.compose.${module["family"] ?: "ui"}"
            dokka(nativeBridge.port("$upstream:$artifact"))
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
