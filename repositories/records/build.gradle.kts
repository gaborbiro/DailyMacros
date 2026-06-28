plugins {
    id("AndroidLibraryConvention")
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.gaborbiro.dailymacros.repositories.records"
}

dependencies {
    implementation(project(":repositories:records:domain"))
    implementation(project(":core:analytics"))
    implementation(project(":data:db"))
    implementation(project(":data:image"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.network.gson)
    implementation(libs.javax.inject)

    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)

    testImplementation(libs.test.junit)
}
