plugins {
    install(libs.plugins.kotlinJvm)
}

template(object : KotlinJvmTemplate() {
    override fun KotlinJvmSourceSetsScope.source() {
        main.configure {
            lib(
                ExportLib,
                projects.ylcsModule.core.base,
            )
        }
    }
})