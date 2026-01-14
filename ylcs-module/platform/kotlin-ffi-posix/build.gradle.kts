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
                projects.ylcsModule.platform.kotlinFfi,
            )
        }

        test.configure {
            lib(
                libs.test,
                libs.kotlinx.coroutines.test,
            )
        }
    }
})