pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "OmniCastPro"

// Módulos del Sistema Clean Architecture
include(":core:domain")
include(":core:protocol")
include(":app-sender")
include(":app-receiver")
