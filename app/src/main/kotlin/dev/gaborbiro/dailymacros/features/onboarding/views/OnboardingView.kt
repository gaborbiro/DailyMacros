package dev.gaborbiro.dailymacros.features.onboarding.views

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.design.LocalExtraColorScheme
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.features.common.utils.verticalScrollWithBar
import dev.gaborbiro.dailymacros.features.common.views.PreviewContext
import dev.gaborbiro.dailymacros.features.overview.views.AddWidgetButton
import dev.gaborbiro.dailymacros.features.overview.views.MacroChip
import dev.gaborbiro.dailymacros.features.overview.views.PhoneIllustration
import dev.gaborbiro.dailymacros.features.overview.views.WelcomeBullet
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.ActivityLevel
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.AgeBracket
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.DietaryFocus
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.GOALS_QUESTIONNAIRE_PAGE_COUNT
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.Gender
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.Goal
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.GoalsQuestionnaireAnswers
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.canAdvanceFromQuestionnairePage
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.views.ActivityLevelPage
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.views.AgeBracketPage
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.views.DietaryFocusPage
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.views.GenderPage
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.views.GoalPage
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.views.ResultPage
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.MacroType
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.TargetUiModel
import kotlinx.coroutines.launch
import dev.gaborbiro.dailymacros.features.overview.R as OverviewR

// The goals questionnaire is flattened directly into onboarding's own pager, starting from its
// 2nd page (index 1) - see the class doc on OnboardingScreen for why it's not a separate route
// when reached this way (it still is one when opened from the Daily Targets screen).
private const val INTRO_PAGE = 0
private const val QUESTIONNAIRE_START_PAGE = 1
private const val QUESTIONNAIRE_END_PAGE = QUESTIONNAIRE_START_PAGE + GOALS_QUESTIONNAIRE_PAGE_COUNT - 1
private const val SETUP_PAGE = QUESTIONNAIRE_END_PAGE + 1
private const val TRIAL_PAGE = SETUP_PAGE + 1
private const val PAGE_COUNT = TRIAL_PAGE + 1

@Composable
internal fun OnboardingView(
    onAddWidget: () -> Unit = {},
    onRestoreFromCloud: () -> Unit = {},
    restoreFromCloudInProgress: Boolean = false,
    onRestoreFromLocalBackup: () -> Unit = {},
    restoreFromLocalBackupInProgress: Boolean = false,
    goalsAnswers: GoalsQuestionnaireAnswers = GoalsQuestionnaireAnswers(),
    goalsPresetTargets: Map<MacroType, TargetUiModel> = emptyMap(),
    onGoalSelected: (Goal) -> Unit = {},
    onGenderSelected: (Gender) -> Unit = {},
    onAgeBracketSelected: (AgeBracket) -> Unit = {},
    onActivityLevelSelected: (ActivityLevel) -> Unit = {},
    onDietaryFocusToggled: (DietaryFocus) -> Unit = {},
    onDietaryFocusNoneTapped: () -> Unit = {},
    onPresetTargetChanged: (MacroType, TargetUiModel) -> Unit = { _, _ -> },
    onPersistGoalsPresetTargets: () -> Unit = {},
    onStartTrialTapped: () -> Unit = {},
    onSkipTapped: () -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()
    val goToPage: (Int) -> Unit = { page ->
        coroutineScope.launch { pagerState.animateScrollToPage(page) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            // Reserves its height on every page (rather than only questionnaire ones) so the
            // pager below never shifts size as the user moves back and forth.
            val onQuestionnairePage = pagerState.currentPage in QUESTIONNAIRE_START_PAGE..QUESTIONNAIRE_END_PAGE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    modifier = Modifier.alpha(if (onQuestionnairePage) 1f else 0f),
                    enabled = onQuestionnairePage,
                    onClick = { goToPage(SETUP_PAGE) },
                ) {
                    Text(stringResource(R.string.onboarding_goals_skip_button))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    INTRO_PAGE -> OnboardingIntroPage()
                    QUESTIONNAIRE_START_PAGE -> GoalPage(selected = goalsAnswers.goal, onSelected = onGoalSelected)
                    QUESTIONNAIRE_START_PAGE + 1 -> GenderPage(selected = goalsAnswers.gender, onSelected = onGenderSelected)
                    QUESTIONNAIRE_START_PAGE + 2 -> AgeBracketPage(selected = goalsAnswers.ageBracket, onSelected = onAgeBracketSelected)
                    QUESTIONNAIRE_START_PAGE + 3 -> ActivityLevelPage(selected = goalsAnswers.activityLevel, onSelected = onActivityLevelSelected)
                    QUESTIONNAIRE_START_PAGE + 4 -> DietaryFocusPage(
                        selected = goalsAnswers.dietaryFocus,
                        reviewed = goalsAnswers.dietaryFocusReviewed,
                        onToggled = onDietaryFocusToggled,
                        onNoneTapped = onDietaryFocusNoneTapped,
                    )
                    QUESTIONNAIRE_END_PAGE -> ResultPage(presetTargets = goalsPresetTargets, onPresetTargetChanged = onPresetTargetChanged)
                    SETUP_PAGE -> OnboardingSetupPage(
                        onAddWidget = onAddWidget,
                        onRestoreFromCloud = onRestoreFromCloud,
                        restoreFromCloudInProgress = restoreFromCloudInProgress,
                        onRestoreFromLocalBackup = onRestoreFromLocalBackup,
                        restoreFromLocalBackupInProgress = restoreFromLocalBackupInProgress,
                    )
                    else -> OnboardingTrialPage(onStartTrialTapped = onStartTrialTapped, onSkipTapped = onSkipTapped)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PageIndicator(pageCount = PAGE_COUNT, currentPage = pagerState.currentPage)
                Spacer(modifier = Modifier.weight(1f))
                // Always rendered (just invisible on the last page) so the row's height - and
                // therefore its vertical position - never shifts between pages.
                val isLastPage = pagerState.currentPage == PAGE_COUNT - 1
                val canAdvance = if (onQuestionnairePage) {
                    canAdvanceFromQuestionnairePage(
                        localPage = pagerState.currentPage - QUESTIONNAIRE_START_PAGE,
                        answers = goalsAnswers,
                        presetTargets = goalsPresetTargets,
                    )
                } else {
                    true
                }
                Button(
                    modifier = Modifier.alpha(if (isLastPage) 0f else 1f),
                    enabled = !isLastPage && canAdvance && !restoreFromCloudInProgress && !restoreFromLocalBackupInProgress,
                    onClick = {
                        if (pagerState.currentPage == QUESTIONNAIRE_END_PAGE) {
                            onPersistGoalsPresetTargets()
                        }
                        goToPage(pagerState.currentPage + 1)
                    },
                ) {
                    Text(stringResource(R.string.onboarding_next))
                }
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

/**
 * The heading sits between two equally weighted halves, so any slack vertical space splits
 * evenly above and below it: it stays centered on tall screens (where the halves have room to
 * spread their own content out) and tight on short ones, without ever needing to scroll.
 */
@Composable
private fun OnboardingIntroPage() {
    val extraColors = LocalExtraColorScheme.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxWidth < 360.dp
        val hPadding = if (compact) 16.dp else 28.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = hPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PhoneIllustration(primaryColor = MaterialTheme.colorScheme.primary)

                    Column(
                        modifier = Modifier.padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MacroChip(label = stringResource(OverviewR.string.welcome_chip_protein), color = extraColors.proteinColor)
                        MacroChip(label = stringResource(OverviewR.string.welcome_chip_carbs), color = extraColors.carbsColor)
                        MacroChip(label = stringResource(OverviewR.string.welcome_chip_fat), color = extraColors.fatColor)
                        MacroChip(label = stringResource(OverviewR.string.welcome_chip_calories), color = extraColors.caloriesColor)
                    }
                }
            }

            Text(
                text = stringResource(OverviewR.string.welcome_heading),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Column(
                    // Sized to the widest bullet line, then centered as a block by the parent -
                    // each bullet stays left-aligned to that block's edge, not to the screen.
                    modifier = Modifier.width(IntrinsicSize.Max),
                    horizontalAlignment = Alignment.Start,
                ) {
                    WelcomeBullet(text = stringResource(OverviewR.string.welcome_bullet_snap))
                    Spacer(modifier = Modifier.height(8.dp))
                    WelcomeBullet(text = stringResource(OverviewR.string.welcome_bullet_ai))
                    Spacer(modifier = Modifier.height(8.dp))
                    WelcomeBullet(text = stringResource(OverviewR.string.welcome_bullet_track))
                }
            }
        }
    }
}

@Composable
private fun OnboardingSetupPage(
    onAddWidget: () -> Unit,
    onRestoreFromCloud: () -> Unit,
    restoreFromCloudInProgress: Boolean = false,
    onRestoreFromLocalBackup: () -> Unit = {},
    restoreFromLocalBackupInProgress: Boolean = false,
) {
    val onBackground = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AddWidgetButton(onClick = onAddWidget)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(OverviewR.string.welcome_widget_hint),
                style = MaterialTheme.typography.bodySmall,
                color = onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.clickable(
                        enabled = !restoreFromCloudInProgress,
                        onClick = onRestoreFromCloud,
                    ),
                    text = stringResource(OverviewR.string.welcome_restore_from_cloud),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (restoreFromCloudInProgress) 0.5f else 1f),
                    textAlign = TextAlign.Center,
                )
                if (restoreFromCloudInProgress) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.clickable(
                        enabled = !restoreFromLocalBackupInProgress,
                        onClick = onRestoreFromLocalBackup,
                    ),
                    text = stringResource(OverviewR.string.welcome_restore_from_local_backup),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (restoreFromLocalBackupInProgress) 0.5f else 1f),
                    textAlign = TextAlign.Center,
                )
                if (restoreFromLocalBackupInProgress) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }

        val uriHandler = LocalUriHandler.current
        val privacyPolicyUrl = stringResource(OverviewR.string.welcome_privacy_policy_url)
        Text(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .clickable { uriHandler.openUri(privacyPolicyUrl) },
            text = stringResource(OverviewR.string.welcome_privacy_policy_link),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OnboardingTrialPage(
    onStartTrialTapped: () -> Unit,
    onSkipTapped: () -> Unit,
) {
    val onBackground = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScrollWithBar()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.onboarding_trial_heading),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboarding_trial_body),
            style = MaterialTheme.typography.bodyLarge,
            color = onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            modifier = Modifier.padding(horizontal = PaddingDefault),
            onClick = onStartTrialTapped,
        ) {
            Text(stringResource(R.string.onboarding_start_trial_button))
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onSkipTapped) {
            Text(stringResource(R.string.onboarding_skip_button))
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun OnboardingViewPreview() {
    PreviewContext {
        OnboardingView()
    }
}
