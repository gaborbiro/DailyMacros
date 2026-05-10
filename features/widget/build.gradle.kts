plugins {
    id("AndroidLibraryConvention")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.gaborbiro.dailymacros.features.widget"
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:design"))
    implementation(project(":features:common"))
    implementation(project(":features:shared"))
    implementation(project(":data:db"))
    implementation(project(":data:file"))
    implementation(project(":data:image"))
    implementation(project(":repositories:records"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.appwidget.preview)
    implementation(libs.androidx.glance.preview)
    implementation(libs.androidx.glance.material3)

    implementation(libs.network.gson)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
