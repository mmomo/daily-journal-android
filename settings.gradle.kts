// Dependencies and plugins come only from Google Maven and Maven Central (DOM-SEC-7).
pluginManagement {
    repositories {
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "daily-journal-android"

include(":domain", ":data", ":app", ":widget")
