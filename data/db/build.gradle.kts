plugins {
    id("AndroidLibraryConvention")
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.gaborbiro.dailymacros.data.db"
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    api(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
