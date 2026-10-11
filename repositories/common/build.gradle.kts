import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

// Shared (KMP) module: the jvm() target is what the Android modules consume today; the iOS targets
// are compiled on macOS only (see docs/kmp/README.md).
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
            // ZonedInstant exposes kotlinx-datetime types in its public API.
            api(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
