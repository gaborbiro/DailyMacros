plugins {
    id("AndroidLibraryConvention")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.gaborbiro.dailymacros.features.common"
}

dependencies {
    implementation(project(":core:design"))
    implementation(project(":data:image"))

    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
}
