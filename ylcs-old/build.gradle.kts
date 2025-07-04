plugins {
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "com.yinlin.rachel"
    compileSdk = rootProject.extra["androidBuildSDK"] as Int

    defaultConfig {
        applicationId = "com.yinlin.rachel"
        minSdk = rootProject.extra["androidMinSDK"] as Int
        targetSdk = rootProject.extra["androidBuildSDK"] as Int
        versionCode = 250
        versionName = "2.5.0"

        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += arrayOf("arm64-v8a")
        }
    }

    signingConfigs {
        register("release") {
            keyAlias = "rachel"
            keyPassword = "rachel1211"
            storeFile = file("key.jks")
            storePassword = "rachel1211"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile(rootProject.extra["r8OptimizeFilename"] as String),
                rootProject.extra["commonR8File"]!!,
                rootProject.extra["androidR8File"]!!
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(libs.mmkv.android)
    implementation(libs.kotlinx.json)
    implementation(libs.kotlinx.io)
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
}