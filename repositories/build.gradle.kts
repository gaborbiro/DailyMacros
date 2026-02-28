import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.gaborbiro.dailymacros.repositories"
    compileSdk = libs.versions.android.sdk.compile.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.sdk.min.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

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
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.java.get())
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":data"))

    implementation(libs.androidx.core.ktx)

    api(libs.network.retrofit)
    api(libs.network.retrofit.converter.gson)
    api(libs.network.okhttp)
    api(libs.network.okhttp.logging.interceptor)
    api(libs.network.okhttp.cookiejar)
    api(libs.network.gson)
}