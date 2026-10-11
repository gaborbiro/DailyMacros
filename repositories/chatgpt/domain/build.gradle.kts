import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(libs.versions.kotlin.jvm.get()))
        }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":repositories:common"))
        }
        // The Dagger qualifiers are Android DI glue, so they live outside commonMain.
        jvmMain.dependencies {
            implementation(libs.javax.inject)
        }
    }
}
