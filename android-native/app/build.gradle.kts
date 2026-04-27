import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ─── Upload key wiring ──────────────────────────────────────────────
// Read keystore credentials from environment variables when present
// (CI). For local builds without env vars set, the release build
// falls back to the debug keystore so `./gradlew assembleRelease`
// keeps working without secrets.
//
// CI sets all four:
//   UPLOAD_KEYSTORE_PATH      – absolute path to the .jks file
//   UPLOAD_KEYSTORE_PASSWORD  – password protecting the keystore
//   UPLOAD_KEY_ALIAS          – alias inside the keystore
//   UPLOAD_KEY_PASSWORD       – password for the alias entry
//
// See android-native/SIGNING.md for how to generate a keystore and
// register it as GitHub Secrets.
val uploadKeystorePath = System.getenv("UPLOAD_KEYSTORE_PATH")
val uploadKeystorePassword = System.getenv("UPLOAD_KEYSTORE_PASSWORD")
val uploadKeyAlias = System.getenv("UPLOAD_KEY_ALIAS")
val uploadKeyPassword = System.getenv("UPLOAD_KEY_PASSWORD")
val hasUploadKey =
    uploadKeystorePath != null &&
    uploadKeystorePassword != null &&
    uploadKeyAlias != null &&
    uploadKeyPassword != null &&
    File(uploadKeystorePath).exists()

android {
    namespace = "com.wordwheel.game"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.wordwheel.game"
        minSdk = 26
        targetSdk = 34
        versionCode = 5
        versionName = "1.4"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        getByName("debug") {
            // Uses default ~/.android/debug.keystore automatically.
            // Force-enable all three signing schemes. AGP otherwise drops v1
            // when minSdk>=24, but many devices (Xiaomi/MIUI, Samsung Secure
            // Folder, older file-manager installers) still reject v2-only
            // APKs with "App not installed as package appears to be invalid".
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
        if (hasUploadKey) {
            create("upload") {
                storeFile = file(uploadKeystorePath!!)
                storePassword = uploadKeystorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use the upload key when CI provides it (Play Store builds);
            // fall back to the debug key for local builds and PR CI runs
            // so contributors can build releases without the secrets.
            signingConfig = if (hasUploadKey) {
                signingConfigs.getByName("upload")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
            assets.srcDirs("../../assets")
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")

    implementation("androidx.core:core-splashscreen:1.0.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
