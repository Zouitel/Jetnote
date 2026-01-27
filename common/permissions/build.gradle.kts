plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.composeVoyager)
    alias(libs.plugins.zouitel.androidCompose)
}

android {
    namespace = "zouitel.neighborhood.permissions"
}

dependencies {
    //Permissions.
    api(libs.permissions.compose)
    api(libs.permissions.camera)
    api(libs.permissions.gallery)
    api(libs.permissions.microphone)
    api(libs.permissions.notifications)
    api(libs.permissions.storage)
}