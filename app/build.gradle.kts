plugins {
    id("com.android.application")

    // Firebase
    id("com.google.gms.google-services")

    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
}

android {
    namespace = "vivek.harman.gaadiipakdo"
    compileSdk = 36

    defaultConfig {
        applicationId = "vivek.harman.gaadiipakdo"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    // =========================
    // AndroidX
    // =========================

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.preference:preference:1.2.1")







    // =========================
    // Material Design
    // =========================

    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.airbnb.android:lottie:6.4.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // =========================
    // Maps
    // =========================

    implementation("org.osmdroid:osmdroid-android:6.1.16")
    implementation(libs.play.services.maps)

    // =========================
    // Firebase
    // =========================

    // Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))

    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth")

    // Firebase Realtime Database
    implementation("com.google.firebase:firebase-database")

    // Firebase Analytics
    implementation("com.google.firebase:firebase-analytics")

    // =========================
    // Testing
    // =========================

    testImplementation(libs.junit)

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}