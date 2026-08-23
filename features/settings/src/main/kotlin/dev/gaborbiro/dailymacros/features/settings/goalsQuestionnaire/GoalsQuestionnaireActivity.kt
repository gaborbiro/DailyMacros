package dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.gaborbiro.dailymacros.design.AppTheme

/**
 * Reachable from the "Retake goals questionnaire" button at the bottom of the Daily Targets
 * screen. A real Activity - stacked on top of MainActivity via the normal Android back stack -
 * rather than a Compose-Navigation route or a Dialog overlay: both of those fought with the
 * Targets sheet's own Dialog window (a visible jump on open, and the questionnaire reopening
 * itself on the way back). Answers are written straight to the settings repository as the user
 * completes the questionnaire (see GoalsQuestionnaireViewModel), so finishing this Activity is
 * enough for MainActivity/Settings to pick them up - no result needs to be returned explicitly.
 */
@AndroidEntryPoint
class GoalsQuestionnaireActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val viewModel: GoalsQuestionnaireViewModel = hiltViewModel()
                GoalsQuestionnaireScreen(
                    viewModel = viewModel,
                    onCloseRequested = { finish() },
                )
            }
        }
    }
}
