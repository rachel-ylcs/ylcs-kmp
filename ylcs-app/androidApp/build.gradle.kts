import java.util.Properties

plugins {
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)
}

dependencies {
    implementation(projects.ylcsApp.shared)
}

kotlin {
    C.useCompilerFeatures(this)

    compilerOptions {
        jvmTarget = C.jvm.androidTarget
    }
}

android {
    namespace = C.app.packageName
    compileSdk = C.android.compileSdk

    compileOptions {
        sourceCompatibility = C.jvm.compatibility
        targetCompatibility = C.jvm.compatibility
    }

    defaultConfig {
        applicationId = C.app.packageName
        minSdk = C.android.minSdk
        targetSdk = C.android.targetSdk
        versionCode = C.app.version
        versionName = C.app.versionName

        ndk {
            for (abi in C.android.ndkAbi) abiFilters += abi
        }
    }

    val androidSigningConfig = try {
        val localProperties = Properties().also { p ->
            C.root.localProperties.asFile.inputStream().use { p.load(it) }
        }
        val androidKeyName = localProperties.getProperty("androidKeyName")
        val androidKeyPassword = localProperties.getProperty("androidKeyPassword")
        signingConfigs {
            register(androidKeyName) {
                keyAlias = androidKeyName
                keyPassword = androidKeyPassword
                storeFile = C.root.config.androidKey.asFile
                storePassword = androidKeyPassword
            }
        }
        signingConfigs.getByName(androidKeyName)
    } catch (e: Throwable) {
        println("Can't load android signing config, error: ${e.message}")
        null
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            signingConfig = androidSigningConfig
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile(C.proguard.defaultRule),
                C.root.app.commonR8Rule, C.root.app.androidR8Rule
            )
            signingConfig = androidSigningConfig
        }
    }

    packaging {
        resources {
            excludes += C.excludes
        }

        dex {
            useLegacyPackaging = true
        }

        jniLibs {
            useLegacyPackaging = true
        }
    }
}

afterEvaluate {
    val assembleDebug = tasks.named("assembleDebug")

    val assembleRelease = tasks.named("assembleRelease")

    val androidCopyAPK by tasks.registering {
        mustRunAfter(assembleRelease)
        doLast {
            copy {
                from(C.root.androidApp.originOutput)
                into(C.root.outputs)
                rename { _ -> "[Android]${C.app.displayName}${C.app.versionName}.APK" }
            }
        }
    }

    // 发布安卓安装包
    val androidPublish by tasks.registering {
        dependsOn(assembleRelease)
        dependsOn(androidCopyAPK)
    }
}