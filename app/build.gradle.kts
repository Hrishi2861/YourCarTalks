import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
keystorePropertiesFile.takeIf { it.exists() }?.inputStream()?.use { keystoreProperties.load(it) }

android {
    namespace = "com.hrishi.yourcartalks"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hrishi.yourcartalks"
        minSdk = 29
        targetSdk = 34
        versionCode = 4
        versionName = "1.4"
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
        }
    }

    signingConfigs {
        create("release") {
            storeFile = keystorePropertiesFile.takeIf { it.exists() }?.let {
                file(keystoreProperties.getProperty("storeFile", ""))
            }
            storePassword = keystoreProperties.getProperty("storePassword", "")
            keyAlias = keystoreProperties.getProperty("keyAlias", "")
            keyPassword = keystoreProperties.getProperty("keyPassword", "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("release")
            } else null
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")

    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Android Auto - CarConnection (detect connection, no CarAppService needed)
    implementation("androidx.car.app:app:1.4.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Sherpa-ONNX offline TTS
    implementation(files("libs/sherpa-onnx-1.13.2.aar"))

    // Bzip2/tar extraction for model download
    implementation("org.apache.commons:commons-compress:1.26.0")
}
