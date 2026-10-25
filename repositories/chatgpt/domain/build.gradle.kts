import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":repositories:common"))
        }
        // Dagger qualifiers are JVM-only DI glue; keep them out of commonMain
        jvmMain.dependencies {
            implementation(libs.javax.inject)
        }
    }
}
