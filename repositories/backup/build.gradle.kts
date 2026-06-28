plugins {
    id("AndroidLibraryConvention")
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.gaborbiro.dailymacros.repositories.backup"
}

dependencies {
    implementation(project(":repositories:backup:domain"))
    implementation(project(":data:db"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.commons.compress)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.network.okhttp)
    implementation(libs.network.gson)

    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)
}
