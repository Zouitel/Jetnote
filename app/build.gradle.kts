import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.zouitel.androidApplication)
    alias(libs.plugins.ksp)
    alias(libs.plugins.licenses)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.firebaseCrashlytics)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "zouitel.neighborhood.jetnote"
    compileSdk = 37

    defaultConfig {
        applicationId = "zouitel.neighborhood.jetnote"
        minSdk = 28
        targetSdk = 36
        versionCode = 10_7_36
        versionName = "10.7.36"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
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

    baselineProfile {\n        automaticGenerationDuringBuild = false\n    }\n\n    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions.optIn.add("org.koin.core.annotation.KoinExperimentalAPI")
    }
}

dependencies {\n    baselineProfile(project(":benchmark"))\n    implementation(libs.androidx.profileinstaller)\n\n    // Local JVM Compose screenshot regression tests.\n    testImplementation(platform(libs.compose.bom))\n    testImplementation(libs.ui)\n    testImplementation(libs.compose.foundation)\n    testImplementation(libs.material3)\n    testImplementation(libs.ui.test.junit4)\n    testImplementation(libs.roborazzi)\n    testImplementation(libs.roborazzi.compose)\n    testImplementation(libs.robolectric)\n    testImplementation(libs.androidx.junit)\n    testImplementation(libs.androidx.espresso.core)\n
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

    //RevenueCat.
//    implementation(libs.revenuecat)
//    implementation(libs.revenuecat.ui)
//    implementation("com.android.billingclient:billing:8.3.0")
}