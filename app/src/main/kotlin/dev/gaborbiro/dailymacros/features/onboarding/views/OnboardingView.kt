package dev.gaborbiro.dailymacros.features.onboarding.views

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.launch
import dev.gaborbiro.dailymacros.features.overview.R as OverviewR

private const val PAGE_COUNT = 2

@Composable
internal fun OnboardingView(
    onAddWidget: () -> Unit = {},
    onRestoreFromCloud: () -> Unit = {},
    onStartTrialTapped: () -> Unit = {},
    onSkipTapped: () -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> OnboardingWelcomePage(onAddWidget = onAddWidget, onRestoreFromCloud = onRestoreFromCloud)
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
                if (pagerState.currentPage == 0) {
                    TextButton(onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } }) {
                        Text(stringResource(R.string.onboarding_next))
                    }
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
private fun OnboardingWelcomePage(
    onAddWidget: () -> Unit,
    onRestoreFromCloud: () -> Unit,
) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    val extraColors = LocalExtraColorScheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScrollWithBar()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(OverviewR.string.welcome_heading),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(20.dp))

        WelcomeBullet(text = stringResource(OverviewR.string.welcome_bullet_snap))
        Spacer(modifier = Modifier.height(8.dp))
        WelcomeBullet(text = stringResource(OverviewR.string.welcome_bullet_ai))
        Spacer(modifier = Modifier.height(8.dp))
        WelcomeBullet(text = stringResource(OverviewR.string.welcome_bullet_track))

        Spacer(modifier = Modifier.height(32.dp))

        AddWidgetButton(onClick = onAddWidget)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(OverviewR.string.welcome_widget_hint),
            style = MaterialTheme.typography.bodySmall,
            color = onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            modifier = Modifier.clickable(onClick = onRestoreFromCloud),
            text = stringResource(OverviewR.string.welcome_restore_from_cloud),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        val uriHandler = LocalUriHandler.current
        val privacyPolicyUrl = stringResource(OverviewR.string.welcome_privacy_policy_url)
        Text(
            text = stringResource(OverviewR.string.welcome_privacy_notice),
            style = MaterialTheme.typography.bodySmall,
            color = onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            modifier = Modifier.clickable { uriHandler.openUri(privacyPolicyUrl) },
            text = stringResource(OverviewR.string.welcome_privacy_policy_link),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))
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
