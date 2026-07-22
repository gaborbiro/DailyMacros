package dev.gaborbiro.dailymacros.features.modal.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportImageToGalleryUseCaseTest {

    @Test
    fun `gallery display name sanitizes colons and keeps extension`() {
        assertEquals(
            "2025-07-28T20-50-19.901788.jpg",
            ExportImageToGalleryUseCase.galleryDisplayName("2025-07-28T20:50:19.901788.png"),
        )
    }

    @Test
    fun `gallery display name falls back when base is blank`() {
        assertEquals(
            "daily-macros-photo.jpg",
            ExportImageToGalleryUseCase.galleryDisplayName(".png"),
        )
    }
}
