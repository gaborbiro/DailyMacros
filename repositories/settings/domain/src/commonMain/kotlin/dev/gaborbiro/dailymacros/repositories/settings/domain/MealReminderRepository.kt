package dev.gaborbiro.dailymacros.repositories.settings.domain

import dev.gaborbiro.dailymacros.repositories.settings.domain.model.MealReminderLastPass
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.MealReminderSlotHistory

/**
 * The meal reminder on/off switch (opt-in, off until the user turns it on), what each meal slot's
 * reminder did recently, and the last reminder pass. Slots are identified by a stable storage key
 * chosen by the caller (e.g. "lunch"), so the stored data survives renaming the caller's types.
 */
interface MealReminderRepository {

    fun isEnabled(): Boolean

    fun setEnabled(enabled: Boolean)

    /** Every key in [slotKeys] is present in the result; a slot with nothing stored gets an empty history. */
    fun getSlotHistory(slotKeys: Collection<String>): Map<String, MealReminderSlotHistory>

    /** Saves every slot in [history] in one write. */
    fun saveSlotHistory(history: Map<String, MealReminderSlotHistory>)

    /** What the last reminder pass did, for the Settings > Debug readout. Null until the first pass. */
    fun getLastPass(): MealReminderLastPass?

    fun setLastPass(lastPass: MealReminderLastPass)
}
