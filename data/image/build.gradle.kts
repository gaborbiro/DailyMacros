plugins {
    id("AndroidLibraryConvention")
}

android {
    namespace = "dev.gaborbiro.dailymacros.data.image"
}

dependencies {
    implementation(project(":data:file"))
    implementation(libs.androidx.core.ktx)
}
