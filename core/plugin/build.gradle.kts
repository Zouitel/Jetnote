plugins {
    alias(libs.plugins.zouitel.androidLibrary)
}

android {
    namespace = "zouitel.neighborhood.plugin"
}

dependencies {
    implementation(libs.play.feature.delivery)
    implementation(libs.coroutines.play.services)
    implementation(libs.coroutines.core)
}
