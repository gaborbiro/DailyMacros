plugins {
    id("AndroidLibraryConvention")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.gaborbiro.dailymacros.features.shared"
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(project(":features:common"))
    implementation(project(":core:design"))
    implementation(project(":core:analytics"))
    implementation(project(":data:image"))
    implementation(project(":repositories:records:domain"))
    implementation(project(":repositories:chatgpt:domain"))
    implementation(project(":repositories:settings:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)

    implementation(libs.dagger.hilt.android)
    implementation(libs.androidx.hilt.work)
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.guava)

    testImplementation(libs.androidx.appcompat)
    testImplementation(libs.test.junit)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
