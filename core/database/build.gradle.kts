plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.ksp)
}

android {
    namespace = "zouitel.neighborhood.database"
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    //Modules.
    implementation(projects.core.repository)

    //Room.
    implementation(libs.roomkts)
    api(libs.room.runtime)
    ksp(libs.room.compiler)

    //Security.
    implementation (libs.security)
}