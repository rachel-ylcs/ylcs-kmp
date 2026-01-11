plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
    )
}

template(object : KotlinJsTemplate() {

})