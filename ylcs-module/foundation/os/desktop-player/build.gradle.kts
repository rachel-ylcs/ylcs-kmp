plugins {
    install(libs.plugins.kotlinJvm)
}

template(object : KotlinJvmTemplate() {
    override fun KotlinJvmSourceSetsScope.source() {
        main.configure {
            lib(
                projects.ylcsModule.platform.nativeLibLoader,
            )
        }
    }
})