plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.androidCompose)
    alias(libs.plugins.zouitel.composeVoyager)
    alias(libs.plugins.kotlin.serialization)
}
android {
    namespace = "zouitel.neighborhood.ui"
}

dependencies {
    //Modules.
    implementation(projects.common.logic)
    implementation(projects.domain)

    //Serialization.
    implementation(libs.kotlinx.serialization.json)

    //Coil.
    implementation(libs.zoomable.coil)

    //QuickJs.
    implementation(libs.quickjs)

    //Markdown.
    implementation(libs.jetbrains.markdown)

    //DateTime.
    implementation(libs.kotlinx.datetime)

    //JLatexMath. (https://github.com/rikkahub/jlatexmath-android)
    implementation(libs.jlatexmath)
    implementation(libs.jlatexmath.font.greek)
    implementation(libs.jlatexmath.font.cyrillic)

    //Jsoup.
    implementation(libs.jsoup)

    //Toast(Sonner).
    implementation(libs.sonner)

    //Liquid Glass (Kyant Backdrop).
    api(libs.backdrop)
}