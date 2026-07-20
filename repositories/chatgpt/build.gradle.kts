plugins {
    id("AndroidLibraryConvention")
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.gaborbiro.dailymacros.repositories.chatgpt"

    // The OpenAI key is no longer embedded in the app. Keyless users go through
    // the Cloud Function proxy (see AuthInterceptor); only the hidden
    // Personalise AI feature lets a user supply their own key at runtime.

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":repositories:common"))
    implementation(project(":repositories:chatgpt:domain"))
    implementation(project(":repositories:settings:domain"))

    implementation(libs.androidx.core.ktx)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    api(libs.network.retrofit)
    api(libs.network.retrofit.converter.gson)
    api(libs.network.okhttp)
    api(libs.network.okhttp.logging.interceptor)
    api(libs.network.okhttp.cookiejar)
    api(libs.network.gson)

    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)

    testImplementation(libs.test.junit)
}
