package dev.gaborbiro.nutrition.app_prefs.domain

import dev.gaborbiro.nutrition.preferences.domain.StoreItem


interface AppPrefs {
    val request: StoreItem<String>
}
