@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.dependencyanalysis) apply false
    alias(libs.plugins.licenses) apply false
    alias(libs.plugins.enigma) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.android.test) apply false
}

buildscript {
    dependencies {
        classpath(libs.enigma)
    }

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
        google()
        gradlePluginPortal()
    }
}

tasks.register("clean", Delete::class){
    delete(rootProject.buildDir)
}