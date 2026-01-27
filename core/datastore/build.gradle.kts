plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.kotlin.serialization)
}


android {
    namespace = "zouitel.neighborhood.datastore"
}
dependencies {
    //Modules.
    implementation(projects.core.repository)

    //DataStore.
    api(libs.datastore)

    //DS Security.
    implementation(libs.datastore.security)

    //Serialization.
    implementation(libs.kotlinx.serialization.json)
}