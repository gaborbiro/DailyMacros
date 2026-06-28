plugins {
    id("AndroidLibraryConvention")
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.gaborbiro.dailymacros.features.modal"
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:design"))
    implementation(project(":core:featureFlags"))
    implementation(project(":features:common"))
    implementation(project(":features:shared"))
    implementation(project(":repositories:chatgpt"))
    implementation(project(":repositories:chatgpt:domain"))
    implementation(project(":repositories:records"))
    implementation(project(":repositories:settings:domain"))
    implementation(project(":data:file"))
    implementation(project(":data:image"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.dagger.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.dagger.hilt.compiler)

    implementation(libs.javax.inject)

    testImplementation(libs.test.junit)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
