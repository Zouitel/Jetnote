plugins {
    alias(libs.plugins.androidDynamicFeature)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "zouitel.neighborhood.speechtranscription"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
    }
}

dependencies {
    implementation(project(":app"))
    implementation(projects.core.plugin)
}
