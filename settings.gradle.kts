pluginManagement {
    includeBuild("build-logic")
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

rootProject.name = "AiAdventChallengeApp"
include(":app")
include(":core:designsystem")
include(":core:database:api")
include(":core:database:impl")
include(":feature:agent:api")
include(":feature:agent:impl")
include(":feature:setting:api")
include(":feature:setting:impl")
