import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "dev.gaborbiro.nutrition.feature.common"
    compileSdk = libs.versions.android.sdk.compile.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.sdk.min.get().toInt()
        consumerProguardFiles("consumer-rules.pro")

        val chatGptApiKeyKey = "CHATGPT_API_KEY"
        val chatGptApiKey = System.getenv(chatGptApiKeyKey) ?: gradleLocalProperties(
            rootDir,
            providers
        ).getProperty(chatGptApiKeyKey) ?: "missing $chatGptApiKeyKey"
        buildConfigField("String", chatGptApiKeyKey, "\"$chatGptApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = libs.versions.enable.proguard.with.release.builds.get().toBoolean()
            isShrinkResources = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        buildConfig = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}


dependencies {
    implementation(project(":data:common"))
    implementation(project(":core:clause"))
}
