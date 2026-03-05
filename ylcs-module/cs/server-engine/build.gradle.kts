plugins {
    install(
        libs.plugins.kotlinJvm,
        libs.plugins.kotlinSerialization,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

template(object : KotlinJvmTemplate() {
    override fun KotlinJvmSourceSetsScope.source() {
        main.configure {
            lib(
                libs.mysql,
                libs.mysql.pool,
                libs.redis,
                libs.ktor.json,
                libs.ktor.client,
                libs.ktor.okhttp,
                libs.ktor.client.negotiation,
                libs.ktor.server.negotiation,
                libs.ktor.server.netty,
                libs.ktor.server.config,
                libs.ktor.server.host,
                libs.ktor.server.websockets,
                ExportLib,
                libs.logback,
                libs.ktor.server,
                projects.ylcsModule.foundation.filesystem,
                projects.ylcsModule.cs.core,
            )
        }
    }
})