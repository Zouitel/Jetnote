plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.androidCompose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "zouitel.neighborhood.markdown"
}

dependencies {
    //Serialization.
    implementation(libs.kotlinx.serialization.json)
}