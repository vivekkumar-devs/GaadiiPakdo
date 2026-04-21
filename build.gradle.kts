plugins {
    // ✅ Correct Android Gradle Plugin (stable)
    id("com.android.application") version "8.9.1" apply false

    // ✅ Firebase Google Services plugin
    id("com.google.gms.google-services") version "4.4.2" apply false
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin) apply false
}