plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":repositories:common"))
    implementation(libs.javax.inject)
}
