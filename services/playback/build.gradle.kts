plugins {
    alias(libs.plugins.zouitel.androidLibrary)
}

android {
    namespace = "zouitel.neighborhood.playback"
}

dependencies {
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
}
