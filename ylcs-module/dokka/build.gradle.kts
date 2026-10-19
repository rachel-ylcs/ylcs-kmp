import love.yinlin.gradle.*
import love.yinlin.repository.C
import org.jetbrains.dokka.gradle.DokkaPlugin

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.dokka,
    )
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
