plugins {
    alias(libs.plugins.zouitel.androidLibrary)
}

android {
    namespace = "zouitel.neighborhood.document"
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}