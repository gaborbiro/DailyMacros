plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// Bump here; GitHub release tag v{versionName} uses :app:printAppReleaseVersionName in CI.
private val appVersionName = "1.10.0"
private val appVersionCode = 24

android {
    namespace = "dev.gaborbiro.dailymacros"
    compileSdk = libs.versions.android.sdk.compile.get().toInt()

    defaultConfig {
        applicationId = "dev.gaborbiro.dailymacros"
        minSdk = libs.versions.android.sdk.min.get().toInt()
        targetSdk = libs.versions.android.sdk.target.get().toInt()
        versionName = appVersionName
        versionCode = appVersionCode

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        getByName("debug") {
            keyAlias = "debug"
            keyPassword = "keystore"
            storeFile = file("../signing/keystore.jks")
            storePassword = "keystore"
        }
        create("release") {
            val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true

            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false

            val releaseConfig = signingConfigs.getByName("release")
            if (releaseConfig.storeFile != null) {
                signingConfig = releaseConfig
            }

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.java.get())
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
    implementation(project(":shared"))
    implementation(project(":core:analytics"))
    implementation(project(":core:design"))
    implementation(project(":features:common"))
    implementation(project(":features:settings"))
    implementation(project(":features:trends"))
    implementation(project(":repositories:chatgpt"))
    implementation(project(":repositories:records"))
    implementation(project(":repositories:settings"))
    implementation(project(":data:db"))
    implementation(project(":data:file"))
    implementation(project(":data:image"))

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


    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.appwidget.preview)
    implementation(libs.androidx.glance.preview)
    implementation(libs.androidx.glance.material3)

    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.accompanist.navigation.animation)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.google.mlkit.image.labeling)

    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.google.guava)

    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    testImplementation(libs.test.junit)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

/** CI reads `app/build/release-version-name.txt` for the GitHub release tag `v{versionName}`. */
tasks.register("writeAppReleaseVersionNameFile") {
    group = "versioning"
    description = "Writes build/release-version-name.txt from appVersionName (for GitHub Actions)"
    val out = layout.buildDirectory.file("release-version-name.txt")
    outputs.file(out)
    doLast {
        out.get().asFile.parentFile.mkdirs()
        out.get().asFile.writeText(appVersionName)
    }
}

afterEvaluate {
    tasks.named("bundleRelease").configure {
        dependsOn("writeAppReleaseVersionNameFile")
    }
}

fun gitShortHash(): String = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
}.standardOutput.asText.get().trim()

fun artifactBaseName(buildType: String): String {
    val versionName = android.defaultConfig.versionName ?: "unknown"
    val versionCode = android.defaultConfig.versionCode
    val commitHash = gitShortHash()
    return "DailyMacros-v${versionName}(${versionCode})-${commitHash}-$buildType"
}

tasks.matching { it.name.startsWith("bundle") }.configureEach {
    doLast {
        val buildType = name.removePrefix("bundle").lowercase()
        val outputDir = File(buildDir, "outputs/bundle/$buildType")
        val original = File(outputDir, "app-$buildType.aab")
        if (!original.exists()) {
            println("⚠️ AAB not found: ${original.absolutePath}")
            return@doLast
        }

        val renamed = File(outputDir, "${artifactBaseName(buildType)}.aab")
        if (original.renameTo(renamed)) {
            println("✅ Renamed ${original.name} → ${renamed.name}")
        } else {
            println("❌ Failed to rename ${original.name}")
        }
    }
}

tasks.matching { it.name.startsWith("assemble") }.configureEach {
    doLast {
        val buildType = name.removePrefix("assemble").lowercase()
        val outputDir = File(buildDir, "outputs/apk/$buildType")
        val original = File(outputDir, "app-$buildType.apk")
        if (!original.exists()) return@doLast

        val renamed = File(outputDir, "${artifactBaseName(buildType)}.apk")
        if (original.renameTo(renamed)) {
            println("✅ Renamed ${original.name} → ${renamed.name}")
        } else {
            println("❌ Failed to rename ${original.name}")
        }
    }
}
