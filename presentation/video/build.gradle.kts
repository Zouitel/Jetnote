plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.androidCompose)
    alias(libs.plugins.zouitel.composeVoyager)
}

android {
    namespace = "zouitel.neighborhood.video"
}

dependencies {
    //Media3.
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
}