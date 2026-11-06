plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.androidCompose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "zouitel.neighborhood.markdown"
}

dependencies {
    testImplementation(libs.junit)
    //Serialization.
    implementation(libs.kotlinx.serialization.json)
}