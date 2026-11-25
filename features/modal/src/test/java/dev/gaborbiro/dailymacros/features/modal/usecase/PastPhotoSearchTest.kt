package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.features.modal.model.PastPhoto
import dev.gaborbiro.dailymacros.repositories.records.domain.model.PhotoUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PastPhotoSearchTest {

    private fun usage(
        filename: String,
        title: String,
        description: String = "",
        representative: Boolean? = null,
        lastUsed: Long,
    ) = PhotoUsage(
        filename = filename,
        mealTitle = title,
        mealDescription = description,
        isRepresentativeOfMeal = representative,
        lastUsedEpochMillis = lastUsed,
    )

    private fun photo(
        filename: String,
        title: String,
        description: String = "",
        packaging: Boolean = false,
        lastUsed: Long,
    ) = listOf(usage(filename, title, description, if (packaging) false else true, lastUsed)).toPastPhoto(filename)

    @Test
    fun `a photo shared by several meals is captioned with the latest one and searchable by all`() {
        val photo = listOf(
            usage("milk.jpg", "Granola with soy milk", lastUsed = 10),
            usage("milk.jpg", "Porridge", description = "oats, soy milk", lastUsed = 30),
            usage("milk.jpg", "Granola with soy milk", lastUsed = 20),
        ).toPastPhoto("milk.jpg")

        assertEquals("Porridge", photo.caption)
        assertEquals(listOf("Porridge", "Granola with soy milk"), photo.mealTitles)
        assertEquals(30L, photo.lastUsedEpochMillis)
        assertTrue("granola" in photo.searchText)
        assertTrue("oats" in photo.searchText)
    }

    @Test
    fun `an untitled meal is captioned with its description`() {
        val photo = listOf(usage("a.jpg", "", description = "Leftover curry", lastUsed = 1)).toPastPhoto("a.jpg")
        assertEquals("Leftover curry", photo.caption)
        assertTrue(photo.mealTitles.isEmpty())
    }

    @Test
    fun `a photo the AI saw as packaging in any meal counts as a packaging shot`() {
        val photo = listOf(
            usage("a.jpg", "Cereal", representative = null, lastUsed = 1),
            usage("a.jpg", "Cereal", representative = false, lastUsed = 2),
        ).toPastPhoto("a.jpg")
        assertTrue(photo.isPackagingShot)
        assertFalse(listOf(usage("b.jpg", "Cereal", representative = true, lastUsed = 1)).toPastPhoto("b.jpg").isPackagingShot)
    }

    @Test
    fun `every word of the query must match, in any order and case`() {
        val photos = listOf(
            photo("1", "Granola with soy milk", lastUsed = 1),
            photo("2", "Soy latte", lastUsed = 2),
            photo("3", "Granola bar", lastUsed = 3),
        )
        assertEquals(listOf("1"), filterPastPhotos(photos, "  MILK  granola ", emptyList()).map { it.filename })
    }

    @Test
    fun `a blank query lists packaging shots first, then the most recent`() {
        val photos = listOf(
            photo("old-dish", "Pasta", lastUsed = 1),
            photo("new-dish", "Salad", lastUsed = 4),
            photo("old-label", "Oat milk", packaging = true, lastUsed = 2),
            photo("new-label", "Soy milk", packaging = true, lastUsed = 3),
        )
        assertEquals(
            listOf("new-label", "old-label", "new-dish", "old-dish"),
            filterPastPhotos(photos, "", emptyList()).map { it.filename },
        )
    }

    @Test
    fun `title matches rank above description-only matches`() {
        val photos = listOf(
            photo("described", "Breakfast", description = "granola, soy milk", packaging = true, lastUsed = 9),
            photo("titled", "Soy milk", lastUsed = 1),
        )
        assertEquals(listOf("titled", "described"), filterPastPhotos(photos, "soy", emptyList()).map { it.filename })
    }

    @Test
    fun `photos already on the meal are left out`() {
        val photos: List<PastPhoto> = listOf(photo("1", "Soy milk", lastUsed = 1), photo("2", "Soy milk", lastUsed = 2))
        assertEquals(listOf("1"), filterPastPhotos(photos, "", listOf("2")).map { it.filename })
    }
}
