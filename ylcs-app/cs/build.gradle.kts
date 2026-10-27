import love.yinlin.gradle.ExportLib
import love.yinlin.repository.C

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.androidLibrary,
    )
}

rachel {
    multiplatform {
        nativeDesktopTarget = true

        sources {
            commonMain {
                lib(
                    projects.ylcsModule.foundation.cryptography,
                    ExportLib,
                    projects.ylcsModule.cs.core,
                    libs.compose.runtime,
                )
                generateCode("generateConstants") {
                    className = "love.yinlin.Local"
                    code = """
package love.yinlin

import love.yinlin.data.AppInfo

// 由构建脚本自动生成，请勿手动修改
object Local {
    val info = AppInfo(
        appName = "${C.app.name}",
        name = "${C.app.displayName}",
        version = ${C.version},
        versionName = "${C.versionName}",
        minVersion = ${C.app.minVersion},
        minVersionName = "${C.app.minVersionName}",
        packageName = "${C.namespace}",
    )
    
    const val API_BASE_URL: String = "${C.cs.apiUrl}"
}
                    """.trimIndent()
                }
            }
        }
    }
}
