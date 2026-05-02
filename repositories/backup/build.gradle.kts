plugins {
    id("AndroidLibraryConvention")
}

android {
    namespace = "dev.gaborbiro.dailymacros.repositories.backup"
}

dependencies {
    api(project(":repositories:backup:domain"))
    implementation(project(":data:db"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
}
