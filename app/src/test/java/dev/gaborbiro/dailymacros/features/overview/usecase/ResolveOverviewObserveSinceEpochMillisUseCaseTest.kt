package dev.gaborbiro.dailymacros.features.overview.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveOverviewObserveSinceEpochMillisUseCaseTest {

    private val useCase = ResolveOverviewObserveSinceEpochMillisUseCase()

    @Test
    fun `returns window start when not searching`() {
        assertEquals(1000L, useCase.execute(searchBlank = true, windowStartEpochMillis = 1000L))
    }

    @Test
    fun `returns zero when searching`() {
        assertEquals(0L, useCase.execute(searchBlank = false, windowStartEpochMillis = 9999L))
    }
}
