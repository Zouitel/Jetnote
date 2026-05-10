enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("org\\.jetbrains.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.itextsupport.com/android")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Jetnote"

include(":app")
include(":ai")
include(":bin")
include(":domain")
include(":common:logic")
include(":common:permissions")
include(":common:ui")
include(":core")
include(":core:database")
include(":core:datastore")
include(":core:repository")
include(":document")
include(":feedback")
include(":services")
include(":presentation")
include(":presentation:account")
include(":presentation:assistant")
include(":presentation:link")
include(":presentation:audio")
include(":presentation:video")
include(":presentation:waveform")
include(":presentation:jotting")
include(":presentation:recorder")
include(":presentation:search")
include(":presentation:tag")
include(":presentation:widget")
include(":presentation:workspace")
include(":presentation:navigator")
include(":services:playback")
include(":services:print")
include(":services:shortcut")
include(":services:tile")
include(":security")
include(":common:markdown")
