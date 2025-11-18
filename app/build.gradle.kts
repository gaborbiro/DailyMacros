import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "dev.gaborbiro.dailymacros"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.gaborbiro.dailymacros"
        minSdk = 31
        targetSdk = 35
        versionCode = 15
        versionName = "1.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
                arg("room.incremental", "true")
                arg("room.expandProjection", "true")
            }
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
        create("prod") {
            keyAlias = localProperties.getProperty("KEY_ALIAS")
            keyPassword = localProperties.getProperty("KEY_PASSWORD")
            storeFile = file(localProperties.getProperty("KEY_PATH"))
            storePassword = localProperties.getProperty("STORE_PASSWORD")
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

            signingConfig = signingConfigs.getByName("prod")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")

    implementation(platform("androidx.compose:compose-bom:2025.09.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.11.0")

    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.work:work-runtime-ktx:2.10.5")
    implementation("androidx.window:window:1.5.0")

    val cameraxVersion = "1.5.0"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.camera:camera-extensions:$cameraxVersion") // optional

    val lifecycleVersion = "2.9.4"
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")

    val glanceVersion = "1.2.0-beta01"
    implementation("androidx.glance:glance-appwidget:$glanceVersion")
    implementation("androidx.glance:glance-appwidget-preview:$glanceVersion")
    implementation("androidx.glance:glance-preview:$glanceVersion")
    implementation("androidx.glance:glance-material3:$glanceVersion")

    val navVersion = "2.9.5"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")
    implementation("androidx.navigation:navigation-compose:$navVersion")

    implementation("com.google.accompanist:accompanist-navigation-animation:0.36.0")

    implementation("com.google.code.gson:gson:2.13.2")

    val roomVersion = "2.8.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion") // Annotation processor for ROOM

    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.1.0")
    implementation("com.squareup.okhttp3:okhttp-java-net-cookiejar:5.1.0")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("com.google.mlkit:image-labeling:17.0.9")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.10.2")
    implementation("com.google.guava:guava:33.4.8-android")

    val firebaseBom = "34.3.0"
    implementation(platform("com.google.firebase:firebase-bom:$firebaseBom"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")

    val vico = "1.16.1"
    implementation("com.patrykandpatrick.vico:core:$vico")
    implementation("com.patrykandpatrick.vico:compose:$vico")
    implementation("com.patrykandpatrick.vico:compose-m3:$vico")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
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
