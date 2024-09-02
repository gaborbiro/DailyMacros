plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(project(":data:common"))
    implementation(libs.kotlinx.coroutines.core)
}