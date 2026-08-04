plugins {
    id("AndroidLibraryConvention")
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.gaborbiro.dailymacros.repositories.billing"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":repositories:common"))
    implementation(project(":repositories:billing:domain"))

    implementation(libs.androidx.core.ktx)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    implementation(libs.google.play.billing.ktx)

    api(libs.network.okhttp)
    api(libs.network.gson)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)

    testImplementation(libs.test.junit)
}
