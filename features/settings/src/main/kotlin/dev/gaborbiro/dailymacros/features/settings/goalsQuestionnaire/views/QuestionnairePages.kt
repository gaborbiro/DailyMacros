package dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.MacroType
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.TargetUiModel

/**
 * The 6 question/result pages shared by the standalone questionnaire screen (reachable from the
 * Daily Targets screen) and by onboarding, which flattens these same pages into its own pager -
 * kept public and Compose-only (no shell/chrome, no ViewModel access) so either host can place
 * them inside its own pager and drive its own Back/Next/Skip bar around them.
 */

@Composable
internal fun QuestionScaffold(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScrollWithBar()
            .padding(horizontal = 24.dp, vertical = 16.dp),
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
            // Always rendered (just invisible when unselected) so the row's height never changes
            // depending on which option is currently checked - see ChoiceButton usage sites.
            Icon(
                modifier = Modifier.alpha(if (selected) 1f else 0f),
                imageVector = Icons.Default.Check,
                contentDescription = null,
            )
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
fun GoalPage(selected: Goal?, onSelected: (Goal) -> Unit) {
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
fun GenderPage(selected: Gender?, onSelected: (Gender) -> Unit) {
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
fun AgeBracketPage(selected: AgeBracket?, onSelected: (AgeBracket) -> Unit) {
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
fun ActivityLevelPage(selected: ActivityLevel?, onSelected: (ActivityLevel) -> Unit) {
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
fun DietaryFocusPage(
    selected: Set<DietaryFocus>,
    reviewed: Boolean,
    onToggled: (DietaryFocus) -> Unit,
    onNoneTapped: () -> Unit,
) {
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
            ChoiceButton(
                label = stringResource(R.string.goals_questionnaire_dietary_none),
                selected = reviewed && selected.isEmpty(),
                onClick = onNoneTapped,
            )
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
fun ResultPage(
    presetTargets: Map<MacroType, TargetUiModel>,
    onPresetTargetChanged: (MacroType, TargetUiModel) -> Unit,
) {
    QuestionScaffold(
        title = stringResource(R.string.goals_questionnaire_result_title),
        subtitle = stringResource(R.string.goals_questionnaire_result_subtitle),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            CORE_MACRO_ROWS.forEach { (type, label, unit) ->
                presetTargets[type]?.let { target ->
                    ResultRow(label = label, unit = unit, target = target, onChange = { onPresetTargetChanged(type, it) })
                }
            }

            val enabledExtras = EXTRA_MACRO_ROWS.filter { (type, _, _) -> presetTargets[type]?.enabled == true }
            if (enabledExtras.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
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
            // Trailing space so the last row isn't flush against the bottom bar once scrolled all
            // the way down.
            Spacer(modifier = Modifier.height(8.dp))
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
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = target.min?.toString() ?: "",
                onValueChange = { onChange(target.copy(min = it.toIntOrNull())) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(stringResource(R.string.settings_content_targets_min_label)) },
            )
            Spacer(modifier = Modifier.width(16.dp))
            OutlinedTextField(
                value = target.max?.toString() ?: "",
                onValueChange = { onChange(target.copy(max = it.toIntOrNull())) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(stringResource(R.string.settings_content_targets_max_label)) },
            )
        }
    }
}
