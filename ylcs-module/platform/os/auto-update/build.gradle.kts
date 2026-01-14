plugins {
    install(
        libs.plugins.kotlinJvm,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

template(object : KotlinJvmTemplate() {
    override fun KotlinJvmSourceSetsScope.source() {
        main.configure {
            lib(
                ExportLib,
                projects.ylcsModule.core,
            )
        }
    }
})