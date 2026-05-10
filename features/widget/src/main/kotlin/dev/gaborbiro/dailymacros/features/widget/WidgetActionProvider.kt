package dev.gaborbiro.dailymacros.features.widget

import androidx.glance.action.Action

/**
 * Glance tap targets that delegate to app activities. Implementations are supplied
 * from the application process via [WidgetActionDependency] so this module does not
 * depend on main or modal activities.
 */
interface WidgetActionProvider {

    fun createRecordWithCamera(): Action

    fun createRecordWithImagePicker(): Action

    fun createRecord(): Action

    fun recordImageTapped(recordId: Long): Action

    fun recordBodyTapped(recordId: Long): Action

    fun quickPickImageTapped(templateId: Long): Action

    fun quickPickBodyTapped(templateId: Long): Action

    fun reload(): Action

    fun openApp(): Action
}

/**
 * Composition root supplies real widget actions (modal / main activity intents).
 */
object WidgetActionDependency {
    lateinit var factory: () -> WidgetActionProvider
}
