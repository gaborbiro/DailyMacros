package dev.gaborbiro.dailymacros.features.modal.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.features.modal.model.DialogState

@Composable
fun ImageDialog(
    dialogState: DialogState.ViewImagesDialog,
    onDismissRequested: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequested,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .wrapContentSize()
                .padding(PaddingDefault),
            shape = RoundedCornerShape(16.dp),
        ) {
            Image(
                bitmap = dialogState.bitmap.asImageBitmap(),
                contentDescription = dialogState.title,
            )
        }
    }
}
