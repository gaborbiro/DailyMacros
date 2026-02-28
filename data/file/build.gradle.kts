plugins {
    id("AndroidLibraryConvention")
}

android {
    namespace = "dev.gaborbiro.dailymacros.data.file"
}

dependencies {
    implementation(libs.androidx.core.ktx)
}
