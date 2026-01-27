plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.androidCompose)
    alias(libs.plugins.zouitel.composeVoyager)
}


android {
    namespace = "zouitel.neighborhood.recorder"

}
    dependencies {
        //Modules.
        implementation(projects.domain)
        implementation(projects.common.ui)
        implementation(projects.common.logic)
        implementation(projects.core.datastore)
        implementation(projects.presentation.audio)
        implementation(projects.feedback)

        //Media3.
        implementation(libs.media3.exoplayer)

        //Cloudy.
        implementation(libs.compose.cloudy)
    }