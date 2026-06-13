plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.argh.hang"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.argh.hang"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "0.2.0"
    }

    buildTypes {
        release {
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
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // WorkManager — recovery scheduling and service watchdog
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    // Coil — image loading for the overlay ImageView
    implementation("io.coil-kt:coil:2.6.0")
    // Activity result contracts (image picker)
    implementation("androidx.activity:activity-ktx:1.9.0")
}
