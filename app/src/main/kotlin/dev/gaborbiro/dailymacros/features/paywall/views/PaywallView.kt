package dev.gaborbiro.dailymacros.features.paywall.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.features.common.utils.verticalScrollWithBar
import dev.gaborbiro.dailymacros.features.common.views.PreviewContext
import dev.gaborbiro.dailymacros.repositories.billing.domain.model.SubscriptionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PaywallView(
    subscriptionState: SubscriptionState,
    onBackNavigateRequested: () -> Unit,
    onSubscribeTapped: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackNavigateRequested) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.paywall_back_cd),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScrollWithBar()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val onBackground = MaterialTheme.colorScheme.onBackground
            if (subscriptionState == SubscriptionState.Active) {
                Text(
                    text = stringResource(R.string.paywall_subscribed_heading),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = stringResource(R.string.paywall_heading),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(R.string.paywall_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = onBackground.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )

                Button(
                    modifier = Modifier.padding(top = 32.dp, start = PaddingDefault, end = PaddingDefault),
                    onClick = onSubscribeTapped,
                ) {
                    Text(stringResource(R.string.paywall_subscribe_button))
                }
            }
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun PaywallViewPreview() {
    PreviewContext {
        PaywallView(
            subscriptionState = SubscriptionState.NotSubscribed,
            onBackNavigateRequested = {},
            onSubscribeTapped = {},
        )
    }
}
