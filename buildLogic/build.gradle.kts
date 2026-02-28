plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
}

repositories {
    google()
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("AndroidLibraryConvention") {
            id = "AndroidLibraryConvention"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
    }
}

dependencies {
    implementation("com.android.tools.build:gradle:9.0.1")
}
