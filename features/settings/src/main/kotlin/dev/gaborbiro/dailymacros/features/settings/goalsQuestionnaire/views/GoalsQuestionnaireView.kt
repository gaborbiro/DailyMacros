package dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.views

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.features.common.utils.verticalScrollWithBar
import dev.gaborbiro.dailymacros.features.common.views.NutrientDisplayLine
import dev.gaborbiro.dailymacros.features.settings.R
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.ActivityLevel
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.AgeBracket
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.DietaryFocus
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.Gender
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.Goal
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.GoalsQuestionnaireAnswers
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.GoalsQuestionnaireUiState
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.MacroType
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.TargetUiModel
import dev.gaborbiro.dailymacros.features.settings.views.SettingsPreviewContext
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 6

@Composable
fun GoalsQuestionnaireView(
    viewState: GoalsQuestionnaireUiState,
    onGoalSelected: (Goal) -> Unit,
    onGenderSelected: (Gender) -> Unit,
    onAgeBracketSelected: (AgeBracket) -> Unit,
    onActivityLevelSelected: (ActivityLevel) -> Unit,
    onDietaryFocusToggled: (DietaryFocus) -> Unit,
    onPresetTargetChanged: (MacroType, TargetUiModel) -> Unit,
    onAcceptTapped: () -> Unit,
    onCloseTapped: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()
    val answers = viewState.answers

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            IconButton(onClick = onCloseTapped) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.goals_questionnaire_close_cd),
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> GoalPage(selected = answers.goal, onSelected = onGoalSelected)
                    1 -> GenderPage(selected = answers.gender, onSelected = onGenderSelected)
                    2 -> AgeBracketPage(selected = answers.ageBracket, onSelected = onAgeBracketSelected)
                    3 -> ActivityLevelPage(selected = answers.activityLevel, onSelected = onActivityLevelSelected)
                    4 -> DietaryFocusPage(selected = answers.dietaryFocus, onToggled = onDietaryFocusToggled)
                    else -> ResultPage(presetTargets = viewState.presetTargets, onPresetTargetChanged = onPresetTargetChanged)
                }
            }

            val isFirstPage = pagerState.currentPage == 0
            val isLastPage = pagerState.currentPage == PAGE_COUNT - 1
            val canAdvance = when (pagerState.currentPage) {
                0 -> answers.goal != null
                1 -> answers.gender != null
                2 -> answers.ageBracket != null
                3 -> answers.activityLevel != null
                4 -> true
                else -> viewState.presetTargets.isNotEmpty()
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
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
                PageIndicator(pageCount = PAGE_COUNT, currentPage = pagerState.currentPage)
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

@Composable
private fun QuestionScaffold(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScrollWithBar()
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        content()
    }
}

@Composable
private fun ChoiceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    emoji: String? = null,
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingDefault, vertical = PaddingDefault),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (emoji != null) {
                Text(text = emoji, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.width(PaddingDefault))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
            }
        }
    }
}

private val GOAL_OPTIONS = listOf(
    Triple(Goal.LOSE_WEIGHT, "🔥", R.string.goals_questionnaire_goal_lose_weight),
    Triple(Goal.BUILD_MUSCLE, "💪", R.string.goals_questionnaire_goal_build_muscle),
    Triple(Goal.EAT_HEALTHIER, "🥗", R.string.goals_questionnaire_goal_eat_healthier),
    Triple(Goal.JUST_TRACK, "📊", R.string.goals_questionnaire_goal_just_track),
)

@Composable
private fun GoalPage(selected: Goal?, onSelected: (Goal) -> Unit) {
    QuestionScaffold(title = stringResource(R.string.goals_questionnaire_goal_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(PaddingDefault)) {
            GOAL_OPTIONS.forEach { (goal, emoji, labelRes) ->
                ChoiceButton(
                    label = stringResource(labelRes),
                    emoji = emoji,
                    selected = selected == goal,
                    onClick = { onSelected(goal) },
                )
            }
        }
    }
}

private val GENDER_OPTIONS = listOf(
    Gender.MALE to R.string.goals_questionnaire_gender_male,
    Gender.FEMALE to R.string.goals_questionnaire_gender_female,
)

@Composable
private fun GenderPage(selected: Gender?, onSelected: (Gender) -> Unit) {
    QuestionScaffold(title = stringResource(R.string.goals_questionnaire_gender_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(PaddingDefault)) {
            GENDER_OPTIONS.forEach { (gender, labelRes) ->
                ChoiceButton(
                    label = stringResource(labelRes),
                    selected = selected == gender,
                    onClick = { onSelected(gender) },
                )
            }
        }
    }
}

private val AGE_BRACKET_OPTIONS = listOf(
    AgeBracket.UNDER_25 to R.string.goals_questionnaire_age_under_25,
    AgeBracket.AGE_25_40 to R.string.goals_questionnaire_age_25_40,
    AgeBracket.AGE_40_60 to R.string.goals_questionnaire_age_40_60,
    AgeBracket.AGE_60_PLUS to R.string.goals_questionnaire_age_60_plus,
)

@Composable
private fun AgeBracketPage(selected: AgeBracket?, onSelected: (AgeBracket) -> Unit) {
    QuestionScaffold(title = stringResource(R.string.goals_questionnaire_age_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(PaddingDefault)) {
            AGE_BRACKET_OPTIONS.forEach { (ageBracket, labelRes) ->
                ChoiceButton(
                    label = stringResource(labelRes),
                    selected = selected == ageBracket,
                    onClick = { onSelected(ageBracket) },
                )
            }
        }
    }
}

private val ACTIVITY_LEVEL_OPTIONS = listOf(
    Triple(ActivityLevel.SEDENTARY, "🛋️", R.string.goals_questionnaire_activity_sedentary),
    Triple(ActivityLevel.LIGHT, "🚶", R.string.goals_questionnaire_activity_light),
    Triple(ActivityLevel.ACTIVE, "🏃", R.string.goals_questionnaire_activity_active),
    Triple(ActivityLevel.VERY_ACTIVE, "🏋️", R.string.goals_questionnaire_activity_very_active),
)

@Composable
private fun ActivityLevelPage(selected: ActivityLevel?, onSelected: (ActivityLevel) -> Unit) {
    QuestionScaffold(title = stringResource(R.string.goals_questionnaire_activity_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(PaddingDefault)) {
            ACTIVITY_LEVEL_OPTIONS.forEach { (activityLevel, emoji, labelRes) ->
                ChoiceButton(
                    label = stringResource(labelRes),
                    emoji = emoji,
                    selected = selected == activityLevel,
                    onClick = { onSelected(activityLevel) },
                )
            }
        }
    }
}

private val DIETARY_FOCUS_OPTIONS = listOf(
    DietaryFocus.SUGAR to R.string.goals_questionnaire_dietary_sugar,
    DietaryFocus.SALT to R.string.goals_questionnaire_dietary_salt,
    DietaryFocus.SATURATED_FAT to R.string.goals_questionnaire_dietary_saturated_fat,
    DietaryFocus.FIBRE to R.string.goals_questionnaire_dietary_fibre,
)

@Composable
private fun DietaryFocusPage(selected: Set<DietaryFocus>, onToggled: (DietaryFocus) -> Unit) {
    QuestionScaffold(
        title = stringResource(R.string.goals_questionnaire_dietary_title),
        subtitle = stringResource(R.string.goals_questionnaire_dietary_subtitle),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PaddingDefault)) {
            DIETARY_FOCUS_OPTIONS.forEach { (focus, labelRes) ->
                ChoiceButton(
                    label = stringResource(labelRes),
                    selected = focus in selected,
                    onClick = { onToggled(focus) },
                )
            }
        }
    }
}

private val CORE_MACRO_ROWS = listOf(
    Triple(MacroType.CALORIES, "Calories", NutrientDisplayLine.Calories.unit),
    Triple(MacroType.PROTEIN, "Protein", NutrientDisplayLine.Protein.unit),
    Triple(MacroType.CARBS, "Carbs", NutrientDisplayLine.Carb.unit),
    Triple(MacroType.FAT, "Fat", NutrientDisplayLine.Fat.unit),
)

private val EXTRA_MACRO_ROWS = listOf(
    Triple(MacroType.SALT, "Salt", NutrientDisplayLine.Salt.unit),
    Triple(MacroType.FIBRE, "Fibre", NutrientDisplayLine.Fibre.unit),
    Triple(MacroType.SATURATED, "of which saturated", NutrientDisplayLine.OfWhichSaturated.unit),
    Triple(MacroType.SUGAR, "of which sugar", NutrientDisplayLine.OfWhichSugar.unit),
)

@Composable
private fun ResultPage(
    presetTargets: Map<MacroType, TargetUiModel>,
    onPresetTargetChanged: (MacroType, TargetUiModel) -> Unit,
) {
    QuestionScaffold(
        title = stringResource(R.string.goals_questionnaire_result_title),
        subtitle = stringResource(R.string.goals_questionnaire_result_subtitle),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PaddingDefault)) {
            CORE_MACRO_ROWS.forEach { (type, label, unit) ->
                presetTargets[type]?.let { target ->
                    ResultRow(label = label, unit = unit, target = target, onChange = { onPresetTargetChanged(type, it) })
                }
            }

            val enabledExtras = EXTRA_MACRO_ROWS.filter { (type, _, _) -> presetTargets[type]?.enabled == true }
            if (enabledExtras.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.goals_questionnaire_result_extras_heading),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
                enabledExtras.forEach { (type, label, unit) ->
                    presetTargets[type]?.let { target ->
                        ResultRow(label = label, unit = unit, target = target, onChange = { onPresetTargetChanged(type, it) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    unit: String,
    target: TargetUiModel,
    onChange: (TargetUiModel) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "$label ($unit)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedTextField(
                value = target.min?.toString() ?: "",
                onValueChange = { onChange(target.copy(min = it.toIntOrNull())) },
                modifier = Modifier
                    .fillMaxWidth(0.48f)
                    .padding(end = 8.dp),
                singleLine = true,
                label = { Text(stringResource(R.string.settings_content_targets_min_label)) },
            )
            OutlinedTextField(
                value = target.max?.toString() ?: "",
                onValueChange = { onChange(target.copy(max = it.toIntOrNull())) },
                modifier = Modifier
                    .fillMaxWidth(0.48f)
                    .padding(start = 8.dp),
                singleLine = true,
                label = { Text(stringResource(R.string.settings_content_targets_max_label)) },
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
            onPresetTargetChanged = { _, _ -> },
            onAcceptTapped = {},
            onCloseTapped = {},
        )
    }
}
