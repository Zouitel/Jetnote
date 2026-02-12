@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.dependencyanalysis) apply false
    alias(libs.plugins.licenses) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.android.test) apply false
}

buildscript {
    dependencies {
    }

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
        google()
        gradlePluginPortal()
    }
}

// Add after your existing code in root build.gradle.kts
val parentProjects = listOf(":presentation", ":common", ":core", ":services")

parentProjects.forEach { parentPath ->
    project(parentPath) {
        tasks.register("clean", Delete::class) {
            delete(layout.buildDirectory)
        }
    }
}