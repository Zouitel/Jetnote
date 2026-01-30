plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.androidCompose)
    alias(libs.plugins.zouitel.composeVoyager)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.kotlin.serialization)
}
android {
    namespace = "zouitel.neighborhood.feedback"
}

dependencies {
    //Models.
    implementation(projects.domain)
    implementation(projects.common.ui)

    //Ktor.
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    //Coil.
    implementation(libs.coil.compose)

    //Koin.
    implementation(libs.koin.worker)

    //Cloudy.
    implementation(libs.compose.cloudy)

    //Google Drive.
    implementation(libs.google.api.services.drive)
    implementation(libs.google.http.client.gson)
    implementation(libs.play.services.auth)
    implementation("com.google.api-client:google-api-client-android:2.0.0") {
        exclude("org.apache.httpcomponents")
    }
    implementation(libs.identity.jvm)

    //Firebase.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
}