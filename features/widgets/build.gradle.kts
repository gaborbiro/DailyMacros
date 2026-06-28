plugins {
    id("AndroidLibraryConvention")
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.gaborbiro.dailymacros.features.widgets"
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
    implementation(project(":repositories:records:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.compose.material3)

    implementation(libs.dagger.hilt.android)
    implementation(libs.androidx.hilt.work)
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.appwidget.preview)
    implementation(libs.androidx.glance.preview)
    implementation(libs.androidx.glance.material3)

    implementation(libs.network.gson)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
