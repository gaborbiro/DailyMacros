plugins {
    id("AndroidLibraryConvention")
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.gaborbiro.dailymacros.core.analytics"
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
}
