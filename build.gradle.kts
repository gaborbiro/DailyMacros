plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.version.catalog.update)
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.ksp) apply false
}

versionCatalogUpdate {
    sortByKey.set(false)
    versionSelector(nl.littlerobots.vcu.plugin.resolver.VersionSelectors.STABLE)
}
