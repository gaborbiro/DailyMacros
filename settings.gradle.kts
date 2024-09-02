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
include(":core:clause")
include(":core:compose")
include(":core:navigation")
include(":core:viewmodel")
include(":preferences")
include(":preferences:domain")
include(":app_prefs")
include(":app_prefs:domain")
include(":data:common")
include(":data:chatgpt")
include(":data:chatgpt:domain")
include(":feature:common")
include(":feature:home")
