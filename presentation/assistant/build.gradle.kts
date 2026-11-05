plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.androidCompose)
    alias(libs.plugins.zouitel.composeVoyager)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "zouitel.neighborhood.assistant"
}

dependencies {
    //Modules.
    implementation(projects.domain)
    implementation(projects.common.ui)
    implementation(projects.common.logic)
    implementation(projects.common.permissions)
    api(projects.ai)
    implementation(projects.feedback)
    implementation(projects.document)

    //Serialization.
    implementation(libs.kotlinx.serialization.json)

    //Pebble (template engine)
    implementation(libs.pebble)

    //Okhttp.
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.okhttp.logging)

    //Firebase.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.config)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    //QuickJs.
    implementation(libs.quickjs)
    implementation(libs.lifecycle.process)

    //Apache commons
    implementation(libs.commons.text)

    //Cloudy.
    implementation(libs.compose.cloudy)

    //Coil.
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.zoomable.coil)

    //Ucrop.
    implementation(libs.ucrop)
}