package dev.gaborbiro.dailymacros.features.widgets

import androidx.glance.action.Action

/**
 * Glance tap targets that delegate to app activities. The app supplies a [WidgetNavigator]
 * implementation through Hilt so this module does not depend on main or modal activities.
 */
interface WidgetNavigator {

    fun createRecordWithCamera(): Action

    fun createRecordWithImagePicker(): Action

    fun createRecord(): Action

    fun recordImageTapped(recordId: Long): Action

    fun recordBodyTapped(recordId: Long): Action

    fun quickPickImageTapped(templateId: Long): Action

    fun quickPickBodyTapped(templateId: Long): Action

    fun quickPickWidgetTapped(templateId: Long): Action

    fun reload(): Action

    fun openApp(): Action
}
