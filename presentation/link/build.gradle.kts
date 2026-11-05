plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.androidCompose)
    alias(libs.plugins.zouitel.composeVoyager)
}
android {
    namespace = "zouitel.neighborhood.link"
}

dependencies {
    testImplementation(libs.junit)

    //Modules.
    implementation(projects.common.ui)
    implementation(projects.common.logic)
    implementation(projects.domain)

    //WorkManager.
    implementation(libs.workmanager)

    //Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    //Koin
    implementation(libs.koin.worker)

    //Metaprobe.
    implementation(libs.metaprobe.kmp)

    //Ktor.
    implementation(libs.ktor.client.android)

    //Jsoup.
    implementation(libs.jsoup)
}