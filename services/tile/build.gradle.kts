plugins {
    alias(libs.plugins.zouitel.androidLibrary)
}

android {
    namespace = "zouitel.neighborhood.tile"
}

dependencies {
    //Modules.
    implementation(projects.presentation.jotting)
}