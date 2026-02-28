package dev.gaborbiro.dailymacros.features.modal.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingHalf

@Composable
fun EditTargetConfirmationDialogContent(
    count: Int,
    onSubmit: (EditTarget) -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.padding(PaddingDefault)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Update the whole series ($count) or just this one?",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .wrapContentWidth()
                    .weight(1f)
            )
            Spacer(modifier = Modifier.size(PaddingHalf))
            Icon(
                imageVector = Icons.Filled.Cancel,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                contentDescription = "Cancel",
                modifier = Modifier
                    .size(30.dp)
                    .clickable {
                        onCancel()
                    },
            )
        }

        Spacer(modifier = Modifier.height(PaddingDefault))

        Button(
            onClick = { onSubmit(EditTarget.RECORD) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ),
            shape = RoundedCornerShape(50.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = "Update single record")
        }

        Spacer(modifier = Modifier.height(PaddingDefault))

        Button(
            onClick = { onSubmit(EditTarget.TEMPLATE) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ),
            shape = RoundedCornerShape(50.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = "Update series")
        }
    }
}

enum class EditTarget {
    RECORD, TEMPLATE
}
