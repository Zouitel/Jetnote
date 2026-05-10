plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.androidCompose)
    alias(libs.plugins.zouitel.composeVoyager)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "zouitel.neighborhood.search"
}

dependencies {
    //Modules.
    implementation(projects.common.ui)
    implementation(projects.common.logic)
    implementation(projects.common.markdown)
    implementation(projects.domain)
    implementation(projects.presentation.tag)
    implementation(projects.presentation.recorder)
    implementation(projects.presentation.audio)
    implementation(projects.presentation.video)
    implementation(projects.presentation.link)
    implementation(projects.presentation.workspace)
    implementation(projects.services.print)
    implementation(projects.feedback)

    //Coil.
    implementation(libs.coil.compose)

    //Cloudy (blur effect).
    implementation(libs.compose.cloudy)

    //Firebase Auth.
    implementation(libs.firebase.auth)

    //Media3.
    implementation(libs.androidx.media3.common.ktx)

    //Serialization.
    implementation(libs.kotlinx.serialization.json)
}