package dev.gaborbiro.dailymacros.features.shared

import dev.gaborbiro.dailymacros.features.shared.model.NutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.DomainError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class MacrosNotificationTextMapperTest {

    private val mapper = MacrosNotificationTextMapper(NutrientsUiMapper())

    @Test
    fun `mapMacrosPrintout null when breakdown empty`() {
        assertNull(mapper.mapMacrosPrintout(NutrientBreakdown()))
    }

    @Test
    fun `mapMacrosPrintout joins non blank lines`() {
        val s = mapper.mapMacrosPrintout(
            NutrientBreakdown(calories = 100, protein = 5f, salt = 0.5f),
        )
        assertTrue(s != null)
        assertTrue(s!!.contains("100"))
        assertTrue(s.contains("5"))
    }

    @Test
    fun `mapDomainErrorToUserMessage check internet`() {
        assertEquals(
            "Internet connectivity error",
            mapper.mapDomainErrorToUserMessage(DomainError.DisplayMessageToUser.CheckInternetConnection()),
        )
    }

    @Test
    fun `mapDomainErrorToUserMessage contact support`() {
        assertTrue(
            mapper.mapDomainErrorToUserMessage(DomainError.DisplayMessageToUser.ContactSupport())
                .contains("engineers"),
        )
    }

    @Test
    fun `mapDomainErrorToUserMessage custom message`() {
        assertEquals(
            "Hello",
            mapper.mapDomainErrorToUserMessage(DomainError.DisplayMessageToUser.Message("Hello")),
        )
    }

    @Test
    fun `mapDomainErrorToUserMessage try again`() {
        assertTrue(
            mapper.mapDomainErrorToUserMessage(DomainError.DisplayMessageToUser.TryAgain())
                .contains("try again"),
        )
    }
}
