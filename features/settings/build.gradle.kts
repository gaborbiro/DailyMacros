plugins {
    id("AndroidLibraryConvention")
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.gaborbiro.dailymacros.features.settings"
}

dependencies {
    implementation(project(":core:design"))
    implementation(project(":core:featureFlags"))
    implementation(project(":features:common"))
    implementation(project(":features:shared"))
    implementation(project(":repositories:common"))
    implementation(project(":repositories:backup:domain"))
    implementation(project(":repositories:settings:domain"))
    implementation(project(":repositories:records:domain"))
    implementation(project(":repositories:chatgpt:domain"))
    implementation(project(":data:image"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.network.gson)
    implementation(libs.google.play.services.auth)
    implementation(libs.pdfbox.android)
    testImplementation(libs.test.junit)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.androidx.test.core)
}
