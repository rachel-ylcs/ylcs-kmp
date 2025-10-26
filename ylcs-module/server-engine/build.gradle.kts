plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    C.useCompilerFeatures(this)
    C.jvmTarget(this)

    sourceSets {
        main.configure {
            useApi(
                projects.ylcsBase.csCore,
                libs.ktor.server,
                libs.ktor.server.websockets,
            )
            useLib(
                libs.logback,
                libs.mysql,
                libs.mysql.pool,
                libs.redis,
                libs.ktor.json,
                libs.ktor.server.negotiation,
                libs.ktor.server.netty,
                libs.ktor.server.config,
                libs.ktor.server.host,
            )
        }
    }
}