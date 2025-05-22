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
        // The maven.google.com URL is redundant as it's included in the google() repository
        // JitPack repository is only needed if you're using libraries hosted there
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "BlueprintVision"
include(":app")
 