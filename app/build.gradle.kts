import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.zouitel.androidApplication)
    alias(libs.plugins.ksp)
    alias(libs.plugins.licenses)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.firebaseCrashlytics)
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "zouitel.neighborhood.jetnote"
    compileSdk = 36

    defaultConfig {
        applicationId = "zouitel.neighborhood.jetnote"
        minSdk = 27
        targetSdk = 36
        versionCode = 1004
        versionName = "10.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
        }

        debug {
            isDebuggable = false
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions.optIn.add("org.koin.core.annotation.KoinExperimentalAPI")
    }
}

dependencies {
    //Modules.
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.repository)
    implementation(projects.domain)
    implementation(projects.services.shortcut)
    implementation(projects.services.playback)
    implementation(projects.services.tile)
    implementation(projects.common.ui)
    implementation(projects.common.logic)
    implementation(projects.common.permissions)
    implementation(projects.security)
    implementation(projects.presentation.navigator)
    implementation(projects.bin)

    //WorkManager.
    implementation(libs.workmanager)

    //Koin.
    implementation(libs.koin.worker)
    implementation(libs.koin.startup)

    //Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    //Media3.
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    //Firebase.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.config)
}