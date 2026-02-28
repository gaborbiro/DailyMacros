plugins {
    id("AndroidLibraryConvention")
}

android {
    namespace = "dev.gaborbiro.dailymacros.repositories.records"
}

dependencies {
    api(project(":repositories:records:domain"))
    implementation(project(":core:analytics"))
    implementation(project(":data"))

    implementation(libs.androidx.core.ktx)
}
