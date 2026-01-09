plugins {
    install(
        libs.plugins.kotlinJvm,
        libs.plugins.mavenPublish,
    )
}

template(object : KotlinJvmTemplate() {
    override fun KotlinJvmSourceSetsScope.source() {
        main.configure {
            lib(
                projects.ylcsModule.platform.kotlinFfi,
            )
        }
    }
})