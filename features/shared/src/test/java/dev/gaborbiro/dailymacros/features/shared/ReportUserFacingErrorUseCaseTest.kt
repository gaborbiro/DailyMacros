package dev.gaborbiro.dailymacros.features.shared

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsSink
import dev.gaborbiro.dailymacros.repositories.common.model.DomainError
import dev.gaborbiro.dailymacros.repositories.common.model.UsageLimitException
import dev.gaborbiro.dailymacros.repositories.common.model.UsageLimitKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ReportUserFacingErrorUseCaseTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    private val loggedErrors = mutableListOf<Throwable>()

    private val useCase get() = ReportUserFacingErrorUseCase(
        errorUiMapper = ErrorUiMapper(context),
        analyticsLogger = AnalyticsLogger(
            sinks = setOf(object : AnalyticsSink {
                override fun onError(t: Throwable) {
                    loggedErrors += t
                }
            }),
        ),
    )

    @Before
    fun setUp() {
        // AnalyticsLogger also records to Crashlytics, which needs a FirebaseApp.
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("1:1:android:1")
                    .setProjectId("test")
                    .setApiKey("test")
                    .build(),
            )
        }
    }

    @Test
    fun `returns the generic message for an unmapped error`() {
        assertEquals("default", useCase.execute(IllegalStateException("boom"), genericMessage = "default"))
    }

    @Test
    fun `returns the specific message for a mapped error`() {
        val message = useCase.execute(
            DomainError.DisplayMessageToUser.CheckInternetConnection(cause = RuntimeException()),
            genericMessage = "default",
        )

        assertEquals(context.getString(R.string.shared_content_connectivity_error), message)
    }

    @Test
    fun `returns the usage limit message`() {
        val message = useCase.execute(
            DomainError.DisplayMessageToUser.OperationFailed(cause = UsageLimitException(UsageLimitKind.DAILY)),
            genericMessage = "default",
        )

        assertEquals(context.getString(R.string.shared_content_usage_limit_daily), message)
    }

    @Test
    fun `logs the shown message with the original error as its cause`() {
        val error = DomainError.DisplayMessageToUser.CheckInternetConnection(cause = RuntimeException())

        val message = useCase.execute(error, genericMessage = "default")

        val logged = loggedErrors.single() as UserFacingError
        assertEquals(message, logged.message)
        assertSame(error, logged.cause)
    }
}
