plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.ksp)
}

android {
    namespace = "zouitel.neighborhood.database"

    defaultConfig {
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    //Modules.
    implementation(projects.core.repository)
    implementation(projects.security)

    //Room.
    implementation(libs.roomkts)
    api(libs.room.runtime)
    ksp(libs.room.compiler)

    implementation("net.zetetic:sqlcipher-android:4.13.0")
    implementation("androidx.sqlite:sqlite-ktx:2.6.2")
}