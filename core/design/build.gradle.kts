plugins {
    id("AndroidLibraryConvention")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.gaborbiro.dailymacros.design"
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
}
