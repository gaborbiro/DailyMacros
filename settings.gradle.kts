pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("buildLogic")
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
include(":core:analytics")
include(":repositories:chatgpt:domain")
include(":repositories:chatgpt")
include(":repositories:records:domain")
include(":repositories:records")
include(":repositories:settings:domain")
include(":repositories:settings")
include(":data")
