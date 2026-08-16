package dev.gaborbiro.dailymacros.features.overview.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.overview.R

/**
 * Proactive, dismissible subscribe CTA shown on Overview once the user has at least one
 * record and isn't subscribed - the entry point that exists independent of Settings (which
 * stays hidden until then, see [OverviewListTopActions]).
 */
@Composable
internal fun SubscribeBanner(
    modifier: Modifier = Modifier,
    onSubscribeTapped: () -> Unit,
    onDismissed: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PaddingDefault, vertical = PaddingHalf),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(start = PaddingDefault, top = PaddingHalf, bottom = PaddingHalf),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.overview_content_subscribe_banner_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(R.string.overview_content_subscribe_banner_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Button(
                    modifier = Modifier.padding(top = 4.dp),
                    onClick = onSubscribeTapped,
                ) {
                    Text(stringResource(R.string.overview_content_subscribe_banner_cta))
                }
            }
            IconButton(onClick = onDismissed) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.overview_content_subscribe_banner_dismiss_cd),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SubscribeBannerPreview() {
    ViewPreviewContext {
        SubscribeBanner(onSubscribeTapped = {}, onDismissed = {})
    }
}
