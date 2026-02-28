import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("AndroidLibraryConvention")
}

android {
    namespace = "dev.gaborbiro.dailymacros.repositories.chatgpt"

    val localProperties = gradleLocalProperties(rootDir, providers)
    val chatGptApiKeyKey = "CHATGPT_API_KEY"
    val chatGptApiKey = System.getenv(chatGptApiKeyKey)
        ?: localProperties.getProperty(chatGptApiKeyKey)
        ?: "missing $chatGptApiKeyKey"

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("String", chatGptApiKeyKey, "\"$chatGptApiKey\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", chatGptApiKeyKey, "\"$chatGptApiKey\"")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(project(":repositories:chatgpt:domain"))
    implementation(project(":core:analytics"))

    implementation(libs.androidx.core.ktx)

    api(libs.network.retrofit)
    api(libs.network.retrofit.converter.gson)
    api(libs.network.okhttp)
    api(libs.network.okhttp.logging.interceptor)
    api(libs.network.okhttp.cookiejar)
    api(libs.network.gson)
}
