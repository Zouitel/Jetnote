plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.composeVoyager)
}


android {
    namespace = "zouitel.neighborhood.playback"
}

dependencies {
    //Modules.
    implementation(projects.presentation.audio)

    //Media3.
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)

    //Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
}
