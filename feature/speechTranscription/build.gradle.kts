plugins {
    id("com.android.dynamic-feature")
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
