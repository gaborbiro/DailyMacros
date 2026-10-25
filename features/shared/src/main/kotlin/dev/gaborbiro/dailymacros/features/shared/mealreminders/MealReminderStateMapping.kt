package dev.gaborbiro.dailymacros.features.shared.mealreminders

import dev.gaborbiro.dailymacros.repositories.settings.domain.MealReminderRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.MealReminderLastPass
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.MealReminderSlotHistory
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

/** Each slot's [SlotReminderHistory], read from [MealReminderRepository]. */
internal fun MealReminderRepository.history(): Map<MealSlot, SlotReminderHistory> {
    val stored = getSlotHistory(MealSlot.entries.map { it.storageKey() })
    return MealSlot.entries.associateWith { slot -> stored.getValue(slot.storageKey()).toSlotReminderHistory() }
}

internal fun MealReminderRepository.saveHistory(history: Map<MealSlot, SlotReminderHistory>) {
    saveSlotHistory(history.entries.associate { (slot, slotHistory) -> slot.storageKey() to slotHistory.toStored() })
}

internal fun MealReminderRepository.setLastPass(at: ZonedDateTime, summary: String, nextWakeUp: ZonedDateTime?) {
    setLastPass(MealReminderLastPass(at.toInstant().toKotlinInstant(), summary, nextWakeUp?.toInstant()?.toKotlinInstant()))
}

internal fun MealReminderLastPass.toStatusLastPass() = MealReminderStatus.LastPass(
    at = at.toZonedDateTime(),
    summary = summary,
    nextWakeUp = nextWakeUp?.toZonedDateTime(),
)

/** Persisted, so it must stay stable: renaming a [MealSlot] must not orphan its stored history. */
private fun MealSlot.storageKey() = name.lowercase()

private fun MealReminderSlotHistory.toSlotReminderHistory() = SlotReminderHistory(
    lastRemindedAt = lastRemindedAt?.toZonedDateTime(),
    consecutiveIgnored = consecutiveIgnored,
    awaitingOutcome = awaitingOutcome,
)

private fun SlotReminderHistory.toStored() = MealReminderSlotHistory(
    lastRemindedAt = lastRemindedAt?.toInstant()?.toKotlinInstant(),
    consecutiveIgnored = consecutiveIgnored,
    awaitingOutcome = awaitingOutcome,
)

private fun Instant.toZonedDateTime() = ZonedDateTime.ofInstant(toJavaInstant(), ZoneId.systemDefault())
