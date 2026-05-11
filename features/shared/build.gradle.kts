plugins {
    id("AndroidLibraryConvention")
    alias(libs.plugins.kotlin.compose)
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
    implementation(project(":repositories:records:domain"))
    implementation(project(":repositories:chatgpt:domain"))
    implementation(project(":repositories:settings:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.network.okhttp)

    implementation(libs.javax.inject)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
