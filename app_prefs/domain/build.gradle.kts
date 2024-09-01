plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(project(":preferences:domain"))

    implementation(libs.kotlinx.coroutines.core)
}