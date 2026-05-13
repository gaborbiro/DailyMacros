plugins {
    id("AndroidLibraryConvention")
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.gaborbiro.dailymacros.repositories.settings"
}

dependencies {
    api(project(":repositories:settings:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.network.gson)
    implementation(libs.javax.inject)

    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)
}
