plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.composeVoyager)
    alias(libs.plugins.kotlin.serialization)
}


android {
    namespace = "zouitel.neighborhood.logic"
}

dependencies {
    testImplementation(libs.junit)

    // okhttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.okhttp.logging)

    //Serialization.
    implementation(libs.kotlinx.serialization.json)
}