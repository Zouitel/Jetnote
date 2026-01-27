plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.composeVoyager)
}
android {
    namespace = "zouitel.neighborhood.bin"
    compileSdk = 36
}

dependencies {
    //Modules.
    implementation(projects.domain)

    //Koin.
    implementation(libs.koin.worker)

    //Serialization.
    implementation(libs.kotlinx.serialization.json)
}