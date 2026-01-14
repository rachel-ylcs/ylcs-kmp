import org.jetbrains.dokka.gradle.DokkaPlugin

plugins {
    install(libs.plugins.dokka)
}

parent!!.subprojects.forEach { p ->
    if (p != project) p.plugins.withType<DokkaPlugin> {
        dependencies.dokka(p)
    }
}

dokka {
    dokkaPublications.html {
        moduleName = "Rachel"
        moduleVersion = C.app.versionName
        outputDirectory = C.root.docs.dokka
        failOnWarning = false
    }
}