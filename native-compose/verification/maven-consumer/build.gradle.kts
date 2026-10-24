import groovy.json.JsonSlurper

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.nativeBridge)
}

val lock = JsonSlurper().parse(file("../../upstream/lock.json")) as Map<*, *>
val portGroup = lock["group"] as String
val portVersion = lock["portVersion"] as String
val explicit = providers.gradleProperty("native.explicit").getOrElse("false").toBoolean()

kotlin {
    compilerOptions { freeCompilerArgs.add("-Xcollection-literals") }
    mingwX64("windows")
    linuxX64("linux")
    macosArm64("macos")
    sourceSets {
        commonMain.dependencies {
            if (explicit) implementation("$portGroup:skiko:$portVersion")
            else implementation(libs.skiko)
        }
        val nativeMain = create("nativeMain") {
            dependsOn(commonMain.get())
            kotlin.srcDir("../../samples/skiko-offscreen/src/nativeMain/kotlin")
        }
        getByName("windowsMain").dependsOn(nativeMain)
        getByName("linuxMain").dependsOn(nativeMain)
        getByName("macosMain").dependsOn(nativeMain)
    }
}

nativeApplication {
    enabled.set(true)
    baseName.set("maven-skiko-consumer")
    entryPoint.set("love.yinlin.nativecompose.offscreen.main")
    runtime("mingw_x64") {
        artifacts.put("$portGroup:skiko-mingwx64:$portVersion:icudtl@dat", "icudtl.dat")
    }
}
