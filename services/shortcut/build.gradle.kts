plugins {
    alias(libs.plugins.zouitel.androidLibrary)
}


android {
    namespace = "zouitel.neighborhood.shortcut"
}

dependencies {
    //Modules.
    implementation(projects.common.ui)
    implementation(projects.common.logic)
    implementation(projects.presentation.jotting)
}