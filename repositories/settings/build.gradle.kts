plugins {
    id("AndroidLibraryConvention")
}

android {
    namespace = "dev.gaborbiro.dailymacros.repositories.settings"
}

dependencies {
    api(project(":repositories:settings:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.network.gson)
}
