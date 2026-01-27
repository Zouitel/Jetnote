plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.androidCompose)
    alias(libs.plugins.zouitel.composeVoyager)
}

android {
    namespace = "zouitel.neighborhood.widget"
}

dependencies {
    //Modules.
    implementation(projects.domain)
    implementation(projects.common.ui)
    implementation(projects.common.logic)
    implementation(projects.common.permissions)
    implementation(projects.presentation.workspace)
    implementation(projects.presentation.tag)
    implementation(projects.presentation.jotting)
    implementation(projects.presentation.audio)

    //Glance.
    implementation(libs.glance)
    implementation(libs.glance.material3)

    //Coil.
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    //Firebase.
    implementation(libs.firebase.auth)

    //Serialization.
    implementation(libs.kotlinx.serialization.json)
}