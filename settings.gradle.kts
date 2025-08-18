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
        maven {
            url = uri("https://androidx.dev/snapshots/latest/artifacts/repository")
            content {
                includeGroupByRegex("androidx\\..*")
            }
        }
    }
}
rootProject.name = "DailyMacros"
include(":app")
