plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.androidCompose)
    alias(libs.plugins.zouitel.composeVoyager)
    alias(libs.plugins.kotlin.serialization)
}


android {
    namespace = "zouitel.neighborhood.tag"
}

dependencies {
    //Modules.
    implementation(projects.domain)
    implementation(projects.common.ui)
    implementation(projects.common.logic)
    implementation(projects.core.datastore)
    implementation(projects.feedback)

    //LiveData.
    implementation(libs.lifecycle.livedata)

    //Lifecycle.
    implementation(libs.lifecycle.compose.viewmodel)

    //Cloudy.
    implementation(libs.compose.cloudy)

    //Serialization.
    implementation(libs.kotlinx.serialization.json)
}