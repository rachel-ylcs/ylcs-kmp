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
                projects.ylcsCore.csBase,
                libs.ktor.server,
                libs.ktor.server.websockets,
                libs.redis,
            )
            useLib(
                libs.logback,
                libs.mysql,
                libs.mysql.pool,
                libs.ktor.json,
                libs.ktor.server.negotiation,
                libs.ktor.server.netty,
                libs.ktor.server.config,
                libs.ktor.server.host,
            )
        }
    }
}