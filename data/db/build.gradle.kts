plugins {
    id("AndroidLibraryConvention")
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.gaborbiro.dailymacros.data.db"
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    api(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.androidx.test.core)
}
