import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

class AndroidLibraryConventionPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        with(project) {
            pluginManager.apply("com.android.library")

            val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
            extensions.configure<LibraryExtension> {
                val compileSdkVer = libs.findVersion("android-sdk-compile").get().requiredVersion
                compileSdk = compileSdkVer.toString().toInt()
                defaultConfig {
                    val minSdkVer = libs.findVersion("android-sdk-min").get().requiredVersion
                    minSdk = minSdkVer.toString().toInt()
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                compileOptions {
                    val javaVersion = libs.findVersion("java").get().requiredVersion.toString()
                    sourceCompatibility = JavaVersion.toVersion(javaVersion)
                    targetCompatibility = JavaVersion.toVersion(javaVersion)
                }
                buildTypes {
                    release {
                        isMinifyEnabled = false
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
                    }
                }
            }
        }
    }
}
