plugins {
    id("AndroidLibraryConvention")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.gaborbiro.dailymacros.features.trends"
}

dependencies {
    implementation(project(":core:design"))
    implementation(project(":features:common"))
    implementation(project(":repositories:records"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
