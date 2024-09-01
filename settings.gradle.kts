pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "Nutrition"
include(":app")
include(":preferences")
include(":core:clause")
include(":core:compose")
include(":core:navigation")
include(":core:viewmodel")
include(":feature:home")
include(":app_prefs")
include(":app_prefs:domain")
include(":preferences:domain")
