plugins {
    alias(libs.plugins.zouitel.androidLibrary)
}

android {
    namespace = "zouitel.neighborhood.root"
}

dependencies {
    //Modules.
    implementation(projects.core.repository)
}