plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.androidCompose)
    alias(libs.plugins.zouitel.composeVoyager)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.gms.google.services)
}
android {
    namespace = "zouitel.neighborhood.account"
}

dependencies {
    //Modules.
    implementation(projects.domain)
    api(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.common.ui)
    implementation(projects.common.logic)
    api(projects.presentation.assistant)
    api(projects.presentation.tag)
    api(projects.presentation.recorder)
    implementation(projects.presentation.audio)
    api(projects.presentation.video)
    api(projects.presentation.workspace)
    api(projects.presentation.jotting)
    api(projects.presentation.link)
    api(projects.presentation.widget)
    api(projects.presentation.search)
    api(projects.feedback)

    //Coil.
    implementation(libs.coil.compose)

    //Play Services Auth.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.googleid)

    //Google Drive.
    implementation(libs.google.api.services.drive)
    implementation(libs.google.http.client.gson)
    implementation(libs.play.services.auth)
    implementation("com.google.api-client:google-api-client-android:2.0.0") {
        exclude("org.apache.httpcomponents")
    }

    //Worker.
    implementation(libs.koin.worker)

    //Serialization.
    implementation(libs.kotlinx.serialization.json)
}