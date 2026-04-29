plugins {
    id("org.jetbrains.kotlin.native.cocoapods") version "2.3.20"
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        podfile = rootProject.file("iosApp/Podfile")
        version = "1.0.0"
        summary = "Daily Macros shared Kotlin Multiplatform module"
        homepage = "https://github.com/gaborbiro/dailymacros"
        ios.deploymentTarget = "15.0"
        name = "DailyMacrosShared"
        framework {
            baseName = "Shared"
            isStatic = true
        }
    }
}
