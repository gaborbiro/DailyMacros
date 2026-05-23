package dev.gaborbiro.dailymacros.features.modal.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class MealVariantListModelsTest {

    @Test
    fun `toPickerOptions merges current into chronological order instead of pinning current first`() {
        val zone = ZoneId.of("UTC")
        val newest = ZonedDateTime.of(2024, 1, 15, 12, 0, 0, 0, zone)
        val middle = ZonedDateTime.of(2024, 1, 10, 12, 0, 0, 0, zone)
        val oldest = ZonedDateTime.of(2024, 1, 5, 12, 0, 0, 0, zone)
        val current = MealVariantListRow(1L, "Current", middle, isCurrent = true)
        val others = listOf(
            MealVariantListRow(2L, "Older", oldest, isCurrent = false),
            MealVariantListRow(3L, "Newer", newest, isCurrent = false),
        )
        val options = MealVariantListResult(current, others).toPickerOptions(diaryDayStartHour = 4)
        assertEquals(listOf(3L, 1L, 2L), options.map { it.templateId })
        assertEquals(
            listOf(false, true, false),
            options.map { it.isCurrentVariant },
        )
    }
}
