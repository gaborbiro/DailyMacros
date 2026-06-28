plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":repositories:common"))
    implementation(libs.javax.inject)
}
