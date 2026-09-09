plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.composeVoyager)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "zouitel.neighborhood.bin"
    compileSdk = 37
}

dependencies {
    //Modules.
    implementation(projects.domain)

    //Koin.
    implementation(libs.koin.worker)

    //Serialization.
    implementation(libs.kotlinx.serialization.json)
}