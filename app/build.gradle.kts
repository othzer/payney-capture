import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// local.properties (gitignored, machine-local) wins over the checked-in
// default in gradle.properties -- see local.properties.example.
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}
val apiBaseUrl: String = (localProperties.getProperty("PAYNEY_API_BASE_URL")
    ?: providers.gradleProperty("PAYNEY_API_BASE_URL").orNull
    ?: "http://10.0.2.2:3000")

// Release builds ignore the machine-local debug URL and always target the
// deployed backend, so a distributed APK can never accidentally ship pointing
// at localhost. Overridable via local/gradle properties if the deploy moves.
val releaseApiBaseUrl: String = (localProperties.getProperty("PAYNEY_RELEASE_API_BASE_URL")
    ?: providers.gradleProperty("PAYNEY_RELEASE_API_BASE_URL").orNull
    ?: "https://payney.vercel.app")

// Release signing, machine-local like the API base URL above -- keystore path
// and passwords live in local.properties (gitignored), never committed.
val releaseStoreFile = localProperties.getProperty("RELEASE_STORE_FILE")
val releaseStorePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
val releaseKeyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")

android {
    namespace = "com.otzrlabs.payney.capture"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.otzrlabs.payney.capture"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Override the debug default: distributed APKs hit the deployed backend.
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
            if (releaseStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.exifinterface)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.ui.tooling)
}
