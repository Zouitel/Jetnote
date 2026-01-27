plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.androidCompose)
    alias(libs.plugins.zouitel.composeVoyager)
    alias(libs.plugins.kotlin.serialization)
}
android {
    namespace = "zouitel.neighborhood.audio"
}

dependencies {
    //Modules.
    implementation(projects.core.datastore)
    implementation(projects.domain)
    implementation(projects.common.ui)
    implementation(projects.common.logic)
    implementation(projects.presentation.waveform)

    //Media3.
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)

    //Koin.
    implementation(libs.koin.worker)

    //Coil.
    implementation(libs.coil.compose)
}