plugins {
    id("com.android.application") version "9.0.1" apply false
    id("nl.littlerobots.version-catalog-update") version "1.1.0"
    id("com.android.library") version "9.0.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
    id("com.google.devtools.ksp") version "2.3.4" apply false
}

versionCatalogUpdate {
    sortByKey.set(false)
    versionSelector(nl.littlerobots.vcu.plugin.resolver.VersionSelectors.STABLE)
}