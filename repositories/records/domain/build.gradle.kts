plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":repositories:common"))
    implementation(libs.kotlinx.coroutines.core)
}
