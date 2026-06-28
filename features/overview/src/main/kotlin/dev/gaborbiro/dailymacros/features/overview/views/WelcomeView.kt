package dev.gaborbiro.dailymacros.features.overview.views

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.features.overview.R
import dev.gaborbiro.dailymacros.design.LocalExtraColorScheme
import dev.gaborbiro.dailymacros.features.common.views.PreviewContext

@Composable
internal fun WelcomeView(modifier: Modifier = Modifier, onAddWidget: () -> Unit = {}) {
    val primary = MaterialTheme.colorScheme.primary
    val onBackground = MaterialTheme.colorScheme.onBackground
    val extraColors = LocalExtraColorScheme.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            contentAlignment = Alignment.Center,
        ) {
            PhoneIllustration(primaryColor = primary)

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MacroChip(
                    label = stringResource(R.string.welcome_chip_protein),
                    color = extraColors.proteinColor,
                )
                MacroChip(
                    label = stringResource(R.string.welcome_chip_carbs),
                    color = extraColors.carbsColor,
                )
                MacroChip(
                    label = stringResource(R.string.welcome_chip_fat),
                    color = extraColors.fatColor,
                )
                MacroChip(
                    label = stringResource(R.string.welcome_chip_calories),
                    color = extraColors.caloriesColor,
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.welcome_heading),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(20.dp))

        WelcomeBullet(text = stringResource(R.string.welcome_bullet_snap))
        Spacer(modifier = Modifier.height(8.dp))
        WelcomeBullet(text = stringResource(R.string.welcome_bullet_ai))
        Spacer(modifier = Modifier.height(8.dp))
        WelcomeBullet(text = stringResource(R.string.welcome_bullet_track))

        Spacer(modifier = Modifier.height(32.dp))

        AddWidgetButton(onClick = onAddWidget)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.welcome_widget_hint),
            style = MaterialTheme.typography.bodySmall,
            color = onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PhoneIllustration(primaryColor: Color) {
    val surface = MaterialTheme.colorScheme.surface
    val background = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .width(130.dp)
            .height(210.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(surface)
            .border(2.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center,
    ) {
        // Camera dot at top
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .size(7.dp)
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = 0.6f)),
        )

        // Viewfinder
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(background)
                .offset(y = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(72.dp)) {
                drawFoodBowl(
                    bowlColor = Color(0xFFFF8C42),
                    steamColor = primaryColor.copy(alpha = 0.5f),
                )
            }
            Canvas(modifier = Modifier.matchParentSize()) {
                drawScanBrackets(color = primaryColor)
            }
        }
    }
}

@Composable
private fun MacroChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun WelcomeBullet(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun DrawScope.drawFoodBowl(bowlColor: Color, steamColor: Color) {
    val cx = size.width / 2
    val bowlWidth = size.width * 0.78f
    val bowlHeight = size.height * 0.42f
    val bowlTop = size.height * 0.46f

    // Bowl body (lower half ellipse)
    drawArc(
        color = bowlColor,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(cx - bowlWidth / 2, bowlTop - bowlHeight / 2),
        size = Size(bowlWidth, bowlHeight),
    )

    // Bowl rim (full ellipse)
    drawOval(
        color = bowlColor.copy(alpha = 0.7f),
        topLeft = Offset(cx - bowlWidth / 2, bowlTop - bowlHeight / 4),
        size = Size(bowlWidth, bowlHeight / 2),
        style = Stroke(width = 3.dp.toPx()),
    )

    // Steam wisps
    val steamBaseY = bowlTop - bowlHeight / 2 - 2.dp.toPx()
    val steamLen = 14.dp.toPx()
    for ((xOffset, flip) in listOf(-10.dp.toPx() to 1f, 0f to -1f, 10.dp.toPx() to 1f)) {
        val x = cx + xOffset
        val path = Path().apply {
            moveTo(x, steamBaseY)
            cubicTo(
                x + flip * 5.dp.toPx(), steamBaseY - steamLen * 0.35f,
                x - flip * 5.dp.toPx(), steamBaseY - steamLen * 0.65f,
                x, steamBaseY - steamLen,
            )
        }
        drawPath(
            path = path,
            color = steamColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.drawScanBrackets(color: Color) {
    val bracketLen = 16.dp.toPx()
    val strokeW = 2.5.dp.toPx()
    val inset = 8.dp.toPx()
    val w = size.width
    val h = size.height

    // Top-left
    drawLine(color, Offset(inset, inset), Offset(inset + bracketLen, inset), strokeW)
    drawLine(color, Offset(inset, inset), Offset(inset, inset + bracketLen), strokeW)
    // Top-right
    drawLine(color, Offset(w - inset, inset), Offset(w - inset - bracketLen, inset), strokeW)
    drawLine(color, Offset(w - inset, inset), Offset(w - inset, inset + bracketLen), strokeW)
    // Bottom-left
    drawLine(color, Offset(inset, h - inset), Offset(inset + bracketLen, h - inset), strokeW)
    drawLine(color, Offset(inset, h - inset), Offset(inset, h - inset - bracketLen), strokeW)
    // Bottom-right
    drawLine(color, Offset(w - inset, h - inset), Offset(w - inset - bracketLen, h - inset), strokeW)
    drawLine(color, Offset(w - inset, h - inset), Offset(w - inset, h - inset - bracketLen), strokeW)
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WelcomeViewPreview() {
    PreviewContext {
        WelcomeView()
    }
}
