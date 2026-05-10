package dev.gaborbiro.dailymacros.features.overview.usecase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComputeOverviewHasMoreItemsUseCaseTest {

    private val useCase = ComputeOverviewHasMoreItemsUseCase()

    @Test
    fun `no more when searching`() {
        assertFalse(
            useCase.execute(
                isSearchActive = true,
                previousItemCount = -1,
                currentItemCount = 10,
            ),
        )
    }

    @Test
    fun `has more on first timeline emission`() {
        assertTrue(
            useCase.execute(
                isSearchActive = false,
                previousItemCount = -1,
                currentItemCount = 5,
            ),
        )
    }

    @Test
    fun `no more when expanded window yields no new rows`() {
        assertFalse(
            useCase.execute(
                isSearchActive = false,
                previousItemCount = 10,
                currentItemCount = 10,
            ),
        )
    }

    @Test
    fun `has more when list grows after expanding window`() {
        assertTrue(
            useCase.execute(
                isSearchActive = false,
                previousItemCount = 10,
                currentItemCount = 11,
            ),
        )
    }
}
