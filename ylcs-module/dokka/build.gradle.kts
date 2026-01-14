plugins {
    install(
        libs.plugins.dokka,
    )
}

dependencies {
    

    dokka(projects.ylcsModule.core.base)
    dokka(projects.ylcsModule.core.compose)
    dokka(projects.ylcsModule.core.cs)
}