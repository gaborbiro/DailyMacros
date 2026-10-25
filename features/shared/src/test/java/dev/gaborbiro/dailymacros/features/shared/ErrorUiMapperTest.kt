package dev.gaborbiro.dailymacros.features.shared

import android.content.Context
import dev.gaborbiro.dailymacros.repositories.common.model.DomainError
import dev.gaborbiro.dailymacros.repositories.common.model.UsageLimitException
import dev.gaborbiro.dailymacros.repositories.common.model.UsageLimitKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ErrorUiMapperTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    private val mapper get() = ErrorUiMapper(context)

    @Test
    fun `map check internet connection`() {
        val result = mapper.map(
            DomainError.DisplayMessageToUser.CheckInternetConnection(cause = RuntimeException()),
            genericMessage = "default",
        )
        assertTrue(result.message.contains("internet", ignoreCase = true))
    }

    @Test
    fun `map operation failed uses default`() {
        assertEquals(
            "Couldn't save your entry",
            mapper.map(
                DomainError.DisplayMessageToUser.OperationFailed(),
                genericMessage = "Couldn't save your entry",
            ).message,
        )
    }

    @Test
    fun `map operation failed hides the technical cause`() {
        assertEquals(
            "Couldn't get macros",
            mapper.map(
                DomainError.DisplayMessageToUser.OperationFailed(cause = Exception("HTTP 500 - server_error - boom")),
                genericMessage = "Couldn't get macros",
            ).message,
        )
    }

    @Test
    fun `map non-domain error uses default`() {
        assertEquals(
            "default",
            mapper.map(IllegalStateException("boom"), genericMessage = "default").message,
        )
    }

    @Test
    fun `map keeps the original error as the cause`() {
        val error = DomainError.DisplayMessageToUser.OperationFailed()

        assertSame(error, mapper.map(error, genericMessage = "default").cause)
    }

    @Test
    fun `map usage limit daily shows specific message`() {
        val result = mapper.map(
            DomainError.DisplayMessageToUser.OperationFailed(
                cause = UsageLimitException(UsageLimitKind.DAILY),
            ),
            genericMessage = "default",
        )
        assertEquals(
            context.getString(R.string.shared_content_usage_limit_daily),
            result.message,
        )
    }

    @Test
    fun `map usage limit monthly and unavailable map to their strings`() {
        assertEquals(
            context.getString(R.string.shared_content_usage_limit_monthly),
            mapper.map(
                DomainError.DisplayMessageToUser.OperationFailed(
                    cause = UsageLimitException(UsageLimitKind.MONTHLY),
                ),
                genericMessage = "default",
            ).message,
        )
        assertEquals(
            context.getString(R.string.shared_content_usage_limit_unavailable),
            mapper.map(
                DomainError.DisplayMessageToUser.OperationFailed(
                    cause = UsageLimitException(UsageLimitKind.UNAVAILABLE),
                ),
                genericMessage = "default",
            ).message,
        )
    }

    @Test
    fun `map subscription required shows its own string`() {
        assertEquals(
            context.getString(R.string.shared_content_usage_limit_subscription_required),
            mapper.map(
                DomainError.DisplayMessageToUser.OperationFailed(
                    cause = UsageLimitException(UsageLimitKind.SUBSCRIPTION_REQUIRED),
                ),
                genericMessage = "default",
            ).message,
        )
    }
}
