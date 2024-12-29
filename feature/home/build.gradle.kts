import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "dev.gaborbiro.nutrition.feature.home"
    compileSdk = libs.versions.android.sdk.compile.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.sdk.min.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
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

    flavorDimensions.add("environment")

    productFlavors {
        create("dev") {
            isDefault = true
            dimension = "environment"

            val googleClientIdKey = "GOOGLE_CLIENT_ID"
            val googleClientId = System.getenv(googleClientIdKey)
                ?: gradleLocalProperties(rootDir, providers).getProperty(googleClientIdKey)
                ?: "missing $googleClientIdKey"
            buildConfigField("String", googleClientIdKey, "\"$googleClientId\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    implementation(project(":core:navigation"))
    implementation(project(":core:compose"))
    implementation(project(":core:clause"))
    implementation(project(":core:viewmodel"))
    implementation(project(":app_prefs:domain"))
    implementation(project(":data:chatgpt:domain"))
    implementation(project(":feature:common"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.ui.tooling.preview)

    implementation(libs.hilt)
    implementation(libs.hilt.navigationCompose)
    implementation(libs.play.services.auth.base)
    implementation(libs.play.services.auth)
    kapt(libs.hilt.compiler)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.google.api.client)
    implementation(libs.google.api.keep)

    debugImplementation(libs.ui.tooling)
}
