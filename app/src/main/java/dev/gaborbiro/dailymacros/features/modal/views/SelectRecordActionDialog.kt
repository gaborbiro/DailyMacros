package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.design.ViewPreviewContext

@Composable
fun SelectRecordActionDialog(
    recordId: Long,
    title: String,
    onRepeatTapped: (recordId: Long) -> Unit,
    onDetailsTapped: (recordId: Long) -> Unit,
    onDeleteTapped: (recordId: Long) -> Unit,
    onDismissRequested: () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequested) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
        ) {
            Column(modifier = Modifier.padding(PaddingDefault)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        modifier = Modifier
                            .padding(PaddingDefault),
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(
                    modifier = Modifier
                        .height(PaddingHalf)
                )
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    onClick = { onRepeatTapped(recordId) },
                    colors = normalButtonColors,
                    shape = RoundedCornerShape(50.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_exposure_plus_1),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = "Repeat")
                }

                Spacer(modifier = Modifier.height(PaddingDefault))

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    onClick = { onDetailsTapped(recordId) },
                    colors = normalButtonColors,
                    shape = RoundedCornerShape(50.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_topic),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = "Details")
                }

                Spacer(modifier = Modifier.height(PaddingDefault))

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    onClick = { onDeleteTapped(recordId) },
                    colors = destructiveButtonColors,
                    shape = RoundedCornerShape(50.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = "Delete")
                }
            }
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun SelectRecordActionDialogPreview() {
    ViewPreviewContext {
        SelectRecordActionDialog(
            recordId = 1,
            title = "Pizza",
            onRepeatTapped = {},
            onDetailsTapped = {},
            onDeleteTapped = {},
            onDismissRequested = {},
        )
    }
}
