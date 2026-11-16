plugins {
    alias(libs.plugins.androidDynamicFeature)
}

android {
    namespace = "zouitel.neighborhood.speechtranscription"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
    }
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.workmanager)
    implementation(project(":app"))
    implementation(projects.core.plugin)
}
