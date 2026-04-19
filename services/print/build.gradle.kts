plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.androidCompose)
    alias(libs.plugins.zouitel.composeVoyager)
}

android {
    namespace = "zouitel.neighborhood.print"
}
dependencies {
    //Projects.
    implementation(projects.document)

    //Print Service.
    implementation(libs.androidx.print)

    //Serialization.
    implementation(libs.kotlinx.serialization.json)
}