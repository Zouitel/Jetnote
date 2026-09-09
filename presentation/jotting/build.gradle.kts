plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.androidCompose)
    alias(libs.plugins.zouitel.composeVoyager)
}
android {
    namespace = "zouitel.neighborhood.jotting"
}

dependencies {
    //Modules.
    implementation(projects.domain)
    implementation(projects.common.permissions)
    implementation(projects.common.ui)
    implementation(projects.common.logic)
    implementation(projects.common.markdown)
    implementation(projects.core.datastore)
    implementation(projects.presentation.workspace)
    implementation(projects.presentation.link)
    implementation(projects.presentation.recorder)
    implementation(projects.presentation.audio)
    implementation(projects.presentation.video)

    //LiveData.
    implementation(libs.lifecycle.livedata)

    //Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    //Media3.
    implementation(libs.media3.exoplayer)

    //Serialization.
    implementation(libs.kotlinx.serialization.json)

    //Firebase.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
}