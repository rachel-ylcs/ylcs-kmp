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
                libs.logback,
                libs.ktor.server,
            )
            useLib(
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
            )
        }
    }
}