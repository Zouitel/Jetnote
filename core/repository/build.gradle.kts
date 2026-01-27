plugins {
    alias(libs.plugins.zouitel.androidLibrary)
}
android {
    namespace = "zouitel.neighborhood.repository"
}

dependencies {
    //Modules.
    implementation(projects.domain)

    //Kotlin-Coroutines.
    implementation(libs.coroutines.core)
}