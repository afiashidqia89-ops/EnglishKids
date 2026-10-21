plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.englishkids.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.englishkids.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        val contentUrl = (project.findProperty("CONTENT_BASE_URL") as String?) ?: ""
        val aiUrl = (project.findProperty("AI_PROXY_URL") as String?) ?: ""
        buildConfigField("String", "CONTENT_BASE_URL", "\"$contentUrl\"")
        buildConfigField("String", "AI_PROXY_URL", "\"$aiUrl\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            // folder content/ di root repo = materi bawaan APK sekaligus materi online
            assets.srcDirs("../content")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildTypes {
        release { isMinifyEnabled = false }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
