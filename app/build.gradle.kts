plugins {
    id("com.android.application")

    // ✅ Firebase plugin
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

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.cardview:cardview:1.0.0")

    implementation("com.google.android.material:material:1.11.0")

    // ✅ osmdroid (Map)
    implementation("org.osmdroid:osmdroid-android:6.1.16")

    // ✅ PreferenceManager (required)
    implementation("androidx.preference:preference:1.2.1")

    // =========================
    // ✅ Firebase (FIXED)
    // =========================

    // BoM (manages versions automatically)
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))

    // 🔥 REQUIRED for your current code
    implementation("com.google.firebase:firebase-database")

    // Optional (keep if needed)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")
    implementation(libs.play.services.maps)

    // =========================

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}