import love.yinlin.gradle.*
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
        windowsTarget = true
        linuxTarget = true
        macosTarget = true

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
        version = ${C.app.version},
        versionName = "${C.app.versionName}",
        minVersion = ${C.app.minVersion},
        minVersionName = "${C.app.minVersionName}",
        packageName = "${C.app.packageName}",
    )
    
    const val MAIN_HOST: String = "${C.host.mainHost}"
    const val API_HOST: String = "${C.host.apiHost}"
    const val API_BASE_URL: String = "${C.host.apiUrl}"
}
            """.trimIndent()
                }
            }
        }
    }
}
