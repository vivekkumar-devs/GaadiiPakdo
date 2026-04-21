pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // ✅ enforce clean repo usage
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "GaadiiPakdo"
include(":app")