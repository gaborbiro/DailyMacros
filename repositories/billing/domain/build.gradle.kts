plugins {
    id("AndroidLibraryConvention")
}

// Unlike every other repositories/*/domain module (plain kotlin("jvm")), this one is an
// Android library: Play Billing's launchBillingFlow API is irreducibly typed against
// android.app.Activity, so SubscriptionRepository can't be expressed in a pure-JVM module.
android {
    namespace = "dev.gaborbiro.dailymacros.repositories.billing.domain"
}

dependencies {
    implementation(project(":repositories:common"))
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.core)
}
