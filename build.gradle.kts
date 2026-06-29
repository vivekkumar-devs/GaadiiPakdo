plugins {

    // Android Gradle Plugin
    id("com.android.application") version "8.9.1" apply false

    // Firebase Google Services
    id("com.google.gms.google-services") version "4.4.2" apply false

    // Secrets Gradle Plugin
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin) apply false
}