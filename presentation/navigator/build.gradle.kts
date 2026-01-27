plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.androidCompose)
    alias(libs.plugins.zouitel.composeVoyager)
    alias(libs.plugins.ksp)
    alias(libs.plugins.licenses)
}

android {
    namespace = "zouitel.neighborhood.navigator"

    kotlinOptions {
        freeCompilerArgs += listOf(
            "-Xopt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "-Xopt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }
}

dependencies {
    //Modules.
    implementation(projects.core.datastore)
    implementation(projects.core.database)
    implementation(projects.domain)
    implementation(projects.common.ui)
    implementation(projects.common.logic)
    api(projects.presentation.assistant)
    api(projects.presentation.tag)
    api(projects.presentation.recorder)
    api(projects.presentation.audio)
    api(projects.presentation.video)
    api(projects.presentation.workspace)
    api(projects.presentation.jotting)
    api(projects.presentation.link)
    api(projects.presentation.widget)
    api(projects.presentation.search)
    api(projects.feedback)
    api(projects.presentation.account)
    api(projects.presentation.assistant)
    implementation(projects.security)

    //Material.
    implementation(libs.compose.material)

    //LiveData.
    implementation(libs.lifecycle.livedata)

    //Cloudy.
    implementation(libs.compose.cloudy)

    //Coil.
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    //License.
    implementation (libs.licenses)

    //Firebase.
    implementation(libs.firebase.auth)

    //Google Service.
    implementation(libs.google.api.services.drive)
    implementation(libs.google.http.client.gson)
    implementation(libs.play.services.auth)
    implementation("com.google.api-client:google-api-client-android:2.0.0") {
        exclude("org.apache.httpcomponents")
    }
}