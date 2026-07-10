plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// Bump here; GitHub release tag v{versionName} uses :app:printAppReleaseVersionName in CI.
private val baseVersion = "1.10.0"

android {
    namespace = "dev.gaborbiro.dailymacros"
    compileSdk = libs.versions.android.sdk.compile.get().toInt()

    defaultConfig {
        applicationId = "dev.gaborbiro.dailymacros"
        minSdk = libs.versions.android.sdk.min.get().toInt()
        targetSdk = libs.versions.android.sdk.target.get().toInt()
        // versionCode and versionName are decided centrally in the androidComponents block below.

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        getByName("debug") {
            keyAlias = "debug"
            keyPassword = "keystore"
            storeFile = file("../signing/dev_keystore.jks")
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
            applicationIdSuffix = ".debug"

            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("qa") {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".qa"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "debug"
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
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

private val pipelineId = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: Int.MAX_VALUE

private val sha = (System.getenv("BUILD_SHA") ?: System.getenv("GITHUB_SHA"))?.take(7) ?: "manual"


// Single source of truth for versionCode: release gets the CI run number so Play Store
// uploads always increase; every other build type gets Int.MAX_VALUE so debug/qa installs
// from CI and laptop never collide and always win over whatever is already installed.
// versionName is derived from the same resolved versionCode, so it never shows a
// pipeline id that doesn't match what was actually shipped.
androidComponents {
    onVariants { variant ->
        val versionCode = if (variant.buildType == "release") pipelineId else Int.MAX_VALUE
        variant.outputs.forEach { output ->
            output.versionCode.set(versionCode)
            output.versionName.set("${baseVersion}(${versionCode})-${sha}")
        }
    }
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:design"))
    implementation(project(":core:featureFlags"))
    implementation(project(":features:modal"))
    implementation(project(":features:overview"))
    implementation(project(":features:common"))
    implementation(project(":features:shared"))
    implementation(project(":features:settings"))
    implementation(project(":features:trends"))
    implementation(project(":features:widgets"))
    implementation(project(":repositories:backup"))
    implementation(project(":repositories:chatgpt"))
    implementation(project(":repositories:records:domain"))
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

    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.dagger.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.accompanist.navigation.animation)

    implementation(libs.google.play.services.auth)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.google.mlkit.image.labeling)

    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.google.guava)

    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    testImplementation(project(":repositories:chatgpt:domain"))
    testImplementation(project(":repositories:common"))
    testImplementation(libs.test.junit)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.work.testing)

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
        out.get().asFile.writeText(baseVersion)
    }
}

afterEvaluate {
    tasks.named("bundleRelease").configure {
        dependsOn("writeAppReleaseVersionNameFile")
    }
}

fun artifactBaseName(buildType: String): String {
    val versionCode = if (buildType == "release") pipelineId else Int.MAX_VALUE
    val versionName = "${baseVersion}(${versionCode})-${sha}"
    return "DailyMacros-v${versionName}-$buildType"
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
