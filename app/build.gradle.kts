import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "dev.gaborbiro.dailymacros"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.gaborbiro.dailymacros"
        minSdk = 31
        targetSdk = 35
        versionName = "1.9.1"
        versionCode = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    val localProperties = gradleLocalProperties(rootDir, providers)

    signingConfigs {
        getByName("debug") {
            keyAlias = "debug"
            keyPassword = "keystore"
            storeFile = file("../signing/keystore.jks")
            storePassword = "keystore"
        }
    }

    val chatGptApiKeyKey = "CHATGPT_API_KEY"
    val chatGptApiKey = System.getenv(chatGptApiKeyKey)
        ?: localProperties.getProperty(chatGptApiKeyKey)
        ?: "missing $chatGptApiKeyKey"

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true

            buildConfigField("String", chatGptApiKeyKey, "\"$chatGptApiKey\"")

            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false

            buildConfigField("String", chatGptApiKeyKey, "\"$chatGptApiKey\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(
                JvmTarget.JVM_17
            )
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.window)

    implementation(libs.androidx.camerax.core)
    implementation(libs.androidx.camerax.camera2)
    implementation(libs.androidx.camerax.lifecycle)
    implementation(libs.androidx.camerax.view)
    implementation(libs.androidx.camerax.extensions)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.appwidget.preview)
    implementation(libs.androidx.glance.preview)
    implementation(libs.androidx.glance.material3)

    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.accompanist.navigation.animation)

    implementation(libs.network.gson)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.network.retrofit)
    implementation(libs.network.retrofit.converter.gson)
    implementation(libs.network.okhttp)
    implementation(libs.network.okhttp.logging.interceptor)
    implementation(libs.network.okhttp.cookiejar)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.google.mlkit.image.labeling)

    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.google.guava)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    implementation(libs.vico.core)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    testImplementation(libs.test.junit)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.matching { it.name.startsWith("bundle") }.configureEach {
    doLast {
        val taskName = name // e.g., "bundleDebug", "bundleRelease"
        val buildType = taskName.removePrefix("bundle").lowercase()
        val versionName = android.defaultConfig.versionName ?: "unknown"
        val versionCode = android.defaultConfig.versionCode

        val outputDir = File(buildDir, "outputs/bundle/$buildType")
        val original = File(outputDir, "app-$buildType.aab")
        if (!original.exists()) {
            println("⚠️ AAB not found: ${original.absolutePath}")
            return@doLast
        }

        val newName = "DailyMacros-v${versionName}(${versionCode})-$buildType.aab"
        val renamed = File(outputDir, newName)

        if (original.renameTo(renamed)) {
            println("✅ Renamed ${original.name} → ${renamed.name}")
        } else {
            println("❌ Failed to rename ${original.name}")
        }
    }
}
