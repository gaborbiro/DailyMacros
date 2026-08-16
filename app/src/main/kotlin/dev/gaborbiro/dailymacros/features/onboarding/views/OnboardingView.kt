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
import kotlinx.coroutines.launch
import dev.gaborbiro.dailymacros.features.overview.R as OverviewR

private const val PAGE_COUNT = 3

@Composable
internal fun OnboardingView(
    onAddWidget: () -> Unit = {},
    onRestoreFromCloud: () -> Unit = {},
    restoreFromCloudInProgress: Boolean = false,
    onStartTrialTapped: () -> Unit = {},
    onSkipTapped: () -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> OnboardingIntroPage()
                    1 -> OnboardingSetupPage(
                        onAddWidget = onAddWidget,
                        onRestoreFromCloud = onRestoreFromCloud,
                        restoreFromCloudInProgress = restoreFromCloudInProgress,
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
                val nextPage = pagerState.currentPage + 1
                Button(
                    modifier = Modifier.alpha(if (isLastPage) 0f else 1f),
                    enabled = !isLastPage && !restoreFromCloudInProgress,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(nextPage) } },
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
