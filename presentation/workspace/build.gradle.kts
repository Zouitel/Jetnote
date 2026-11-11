plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.androidCompose)
    alias(libs.plugins.zouitel.composeVoyager)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}
android {
    namespace = "zouitel.neighborhood.workspace"
}

dependencies {
    //Modules.
    implementation(projects.common.ui)
    implementation(projects.common.logic)
    implementation(projects.common.permissions)
    implementation(projects.common.markdown)
    implementation(projects.domain)
    implementation(projects.core.datastore)
    implementation(projects.presentation.recorder)
    implementation(projects.presentation.audio)
    implementation(projects.presentation.video)
    implementation(projects.presentation.tag)
    implementation(projects.presentation.link)
    implementation(projects.services.print)
    implementation(projects.feedback)
    implementation(projects.document)

    //Cloudy.
    implementation(libs.compose.cloudy)

    //Coil.
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.zoomable.coil)

    //Koin.
    implementation(libs.koin.worker)

    //Media3.
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.compose)
    implementation(libs.media3.compose.material3)

    //Media details.
    implementation(libs.androidx.exifinterface)

    //Firebase.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    //Serialization.
    implementation(libs.kotlinx.serialization.json)

    //Datetime.
    implementation(libs.kotlinx.datetime)

    implementation(libs.androidx.recyclerview)

    //DocViewer.
    implementation(libs.docviewer)

    //ML Kit Document Scanner.
    implementation(libs.play.services.mlkit.document.scanner)

    // PDF text extraction for note import.
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
}