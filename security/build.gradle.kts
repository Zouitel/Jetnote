plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.androidCompose)
    alias(libs.plugins.zouitel.composeVoyager)
}

android {
    namespace = "zouitel.neighborhood.security"
}

dependencies {
    //Modules.
    implementation(projects.common.ui)
    implementation(projects.common.logic)
    implementation(projects.domain)

    //Biometric.
    implementation(libs.androidx.biometric)

    //Lifecycle.
    implementation(libs.lifecycle.compose.runtime)
}