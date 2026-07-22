import org.gradle.api.tasks.Copy
import java.io.File

plugins {
    id("AndroidLibraryConvention")
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

val roomTestAssetsDir: File
    get() = File(layout.buildDirectory.get().asFile, "generated/roomTestAssets")

android {
    namespace = "dev.gaborbiro.dailymacros.data.db"
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
    sourceSets.named("test") {
        assets.srcDir(roomTestAssetsDir)
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

tasks.withType<Test>().configureEach {
    // Hilt KSP generates test sources for the test variant, which makes Gradle think test
    // sources are present even when no JUnit tests exist in this module yet.
    failOnNoDiscoveredTests = false
}

val copyRoomExportSchemasForTestAssets = tasks.register<Copy>("copyRoomExportSchemasForTestAssets") {
    from(layout.projectDirectory.dir("schemas"))
    into(roomTestAssetsDir)
}

afterEvaluate {
    val copy = copyRoomExportSchemasForTestAssets
    tasks.matching { it.name.startsWith("merge") && it.name.endsWith("UnitTestAssets") }
        .configureEach { dependsOn(copy) }
    tasks.matching {
        it.name.contains("UnitTest") &&
            (it.name.contains("lint", ignoreCase = true) || it.name.contains("Lint"))
    }.configureEach { dependsOn(copy) }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    api(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)

    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.runner)
    testImplementation(libs.test.junit)
    testImplementation(libs.test.robolectric)
}
