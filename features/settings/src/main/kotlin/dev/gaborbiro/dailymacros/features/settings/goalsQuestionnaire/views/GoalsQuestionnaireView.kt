package dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.views

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.features.settings.R
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.ActivityLevel
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.AgeBracket
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.DietaryFocus
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.GOALS_QUESTIONNAIRE_PAGE_COUNT
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.Gender
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.Goal
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.GoalsQuestionnaireAnswers
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.GoalsQuestionnaireUiState
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.canAdvanceFromQuestionnairePage
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.MacroType
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.TargetUiModel
import dev.gaborbiro.dailymacros.features.settings.views.SettingsPreviewContext
import kotlinx.coroutines.launch

/** The standalone questionnaire screen, reachable from the Daily Targets screen. Onboarding
 *  flattens the same [GoalPage]/[GenderPage]/etc. pages into its own pager and chrome instead of
 *  hosting this composable - see OnboardingView. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsQuestionnaireView(
    viewState: GoalsQuestionnaireUiState,
    onGoalSelected: (Goal) -> Unit,
    onGenderSelected: (Gender) -> Unit,
    onAgeBracketSelected: (AgeBracket) -> Unit,
    onActivityLevelSelected: (ActivityLevel) -> Unit,
    onDietaryFocusToggled: (DietaryFocus) -> Unit,
    onDietaryFocusNoneTapped: () -> Unit,
    onPresetTargetChanged: (MacroType, TargetUiModel) -> Unit,
    onAcceptTapped: () -> Unit,
    onCloseTapped: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { GOALS_QUESTIONNAIRE_PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()
    val answers = viewState.answers
    val isFirstPage = pagerState.currentPage == 0
    val isLastPage = pagerState.currentPage == GOALS_QUESTIONNAIRE_PAGE_COUNT - 1
    val canAdvance = canAdvanceFromQuestionnairePage(pagerState.currentPage, answers, viewState.presetTargets)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onCloseTapped) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.goals_questionnaire_close_cd),
                        )
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        modifier = Modifier.alpha(if (isFirstPage) 0f else 1f),
                        enabled = !isFirstPage,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        },
                    ) {
                        Text(stringResource(R.string.goals_questionnaire_back))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    PageIndicator(pageCount = GOALS_QUESTIONNAIRE_PAGE_COUNT, currentPage = pagerState.currentPage)
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        enabled = canAdvance,
                        onClick = {
                            if (isLastPage) {
                                onAcceptTapped()
                            } else {
                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        },
                    ) {
                        Text(stringResource(if (isLastPage) R.string.goals_questionnaire_accept else R.string.goals_questionnaire_next))
                    }
                }
            }
        },
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) { page ->
            when (page) {
                0 -> GoalPage(selected = answers.goal, onSelected = onGoalSelected)
                1 -> GenderPage(selected = answers.gender, onSelected = onGenderSelected)
                2 -> AgeBracketPage(selected = answers.ageBracket, onSelected = onAgeBracketSelected)
                3 -> ActivityLevelPage(selected = answers.activityLevel, onSelected = onActivityLevelSelected)
                4 -> DietaryFocusPage(
                    selected = answers.dietaryFocus,
                    reviewed = answers.dietaryFocusReviewed,
                    onToggled = onDietaryFocusToggled,
                    onNoneTapped = onDietaryFocusNoneTapped,
                )
                else -> ResultPage(presetTargets = viewState.presetTargets, onPresetTargetChanged = onPresetTargetChanged)
            }
        }
    }
}

@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = if (index == currentPage) 1f else 0.3f)
                    ),
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GoalsQuestionnaireViewPreview() {
    SettingsPreviewContext {
        GoalsQuestionnaireView(
            viewState = GoalsQuestionnaireUiState(answers = GoalsQuestionnaireAnswers()),
            onGoalSelected = {},
            onGenderSelected = {},
            onAgeBracketSelected = {},
            onActivityLevelSelected = {},
            onDietaryFocusToggled = {},
            onDietaryFocusNoneTapped = {},
            onPresetTargetChanged = { _, _ -> },
            onAcceptTapped = {},
            onCloseTapped = {},
        )
    }
}
