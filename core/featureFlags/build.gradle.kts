plugins {
    id("AndroidLibraryConvention")
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.gaborbiro.dailymacros.core.featureflags"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)

    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)
}
