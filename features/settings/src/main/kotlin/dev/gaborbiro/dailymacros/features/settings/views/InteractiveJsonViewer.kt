package dev.gaborbiro.dailymacros.features.settings.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

@Composable
internal fun InteractiveJsonViewer(
    modifier: Modifier = Modifier,
    json: String,
    onCopyAll: () -> Unit,
    expandedBits: String = "",
    onExpandedBitsChange: (String) -> Unit = {},
) {
    if (json.isBlank()) return

    val root = remember(json) { runCatching { JsonParser.parseString(json) }.getOrNull() } ?: return
    val containerPaths = remember(root) {
        buildList {
            collectContainerPaths(root = root, path = "root", out = this)
        }
    }

    JsonViewerHeader(
        modifier = modifier.fillMaxWidth(),
        onCopyAll = onCopyAll,
        onCollapseAll = { onExpandedBitsChange(allCollapsedBits(containerPaths)) },
        onExpandAll = { onExpandedBitsChange(allExpandedBits(containerPaths)) },
    )
    Spacer(modifier = Modifier.height(8.dp))
    SelectionContainer {
        Column {
            JsonNode(
                element = root,
                key = null,
                path = "root",
                indent = 0,
                containerPaths = containerPaths,
                expandedBits = expandedBits,
                onExpandedBitsChange = onExpandedBitsChange,
            )
        }
    }
}

@Composable
private fun JsonViewerHeader(
    modifier: Modifier = Modifier,
    onCopyAll: () -> Unit,
    onCollapseAll: () -> Unit,
    onExpandAll: () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(
            onClick = onCopyAll,
            contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
        ) {
            Text("Copy all")
        }
        TextButton(
            onClick = onCollapseAll,
            contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
        ) {
            Text("Collapse all")
        }
        TextButton(
            onClick = onExpandAll,
            contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
        ) {
            Text("Expand all")
        }
    }
}

@Composable
private fun JsonNode(
    element: JsonElement,
    key: String?,
    path: String,
    indent: Int,
    containerPaths: List<String>,
    expandedBits: String,
    onExpandedBitsChange: (String) -> Unit,
) {
    when {
        element.isJsonObject -> JsonObjectNode(
            key = key,
            obj = element.asJsonObject,
            path = path,
            indent = indent,
            containerPaths = containerPaths,
            expandedBits = expandedBits,
            onExpandedBitsChange = onExpandedBitsChange,
        )

        element.isJsonArray -> JsonArrayNode(
            key = key,
            array = element.asJsonArray,
            path = path,
            indent = indent,
            containerPaths = containerPaths,
            expandedBits = expandedBits,
            onExpandedBitsChange = onExpandedBitsChange,
        )

        else -> JsonPrimitiveNode(
            key = key,
            value = formatJsonValue(element),
            indent = indent,
        )
    }
}

@Composable
private fun JsonObjectNode(
    key: String?,
    obj: JsonObject,
    path: String,
    indent: Int,
    containerPaths: List<String>,
    expandedBits: String,
    onExpandedBitsChange: (String) -> Unit,
) {
    val expanded = isPathExpandedInBits(containerPaths, expandedBits, path)
    JsonContainerHeader(
        indent = indent,
        key = key,
        openBracket = "{",
        closeBracket = "}",
        expanded = expanded,
        onToggle = {
            onExpandedBitsChange(flipExpansionBit(containerPaths, expandedBits, path))
        },
    )

    if (!expanded) return

    obj.entrySet().forEach { (childKey, childValue) ->
        JsonNode(
            element = childValue,
            key = childKey,
            path = "$path.$childKey",
            indent = indent + 1,
            containerPaths = containerPaths,
            expandedBits = expandedBits,
            onExpandedBitsChange = onExpandedBitsChange,
        )
    }
    JsonLine(indent = indent, text = "}")
}

@Composable
private fun JsonArrayNode(
    key: String?,
    array: JsonArray,
    path: String,
    indent: Int,
    containerPaths: List<String>,
    expandedBits: String,
    onExpandedBitsChange: (String) -> Unit,
) {
    val expanded = isPathExpandedInBits(containerPaths, expandedBits, path)
    JsonContainerHeader(
        indent = indent,
        key = key,
        openBracket = "[",
        closeBracket = "]",
        expanded = expanded,
        onToggle = {
            onExpandedBitsChange(flipExpansionBit(containerPaths, expandedBits, path))
        },
    )

    if (!expanded) return

    array.forEachIndexed { index, child ->
        JsonNode(
            element = child,
            key = null,
            path = "$path[$index]",
            indent = indent + 1,
            containerPaths = containerPaths,
            expandedBits = expandedBits,
            onExpandedBitsChange = onExpandedBitsChange,
        )
    }
    JsonLine(indent = indent, text = "]")
}

@Composable
private fun JsonContainerHeader(
    indent: Int,
    key: String?,
    openBracket: String,
    closeBracket: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indent * 12).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = buildString {
                if (key != null) append("\"$key\": ")
                append(openBracket)
                append(" ")
            },
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
        JsonToggleButton(
            expanded = expanded,
            onClick = onToggle,
        )
        if (!expanded) {
            Text(
                text = " $closeBracket",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
internal fun JsonToggleButton(
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val labelStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
    val chipShape = RoundedCornerShape(3.dp)
    val chipColor = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier
            .clip(chipShape)
            .background(chipColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (expanded) "-" else "+",
            style = labelStyle,
        )
    }
}

@Composable
private fun JsonPrimitiveNode(
    key: String?,
    value: String,
    indent: Int,
) {
    JsonLine(
        indent = indent,
        text = buildString {
            if (key != null) append("\"$key\": ")
            append(value)
        },
    )
}

@Composable
private fun JsonLine(
    indent: Int,
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indent * 12).dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
}

private fun collectContainerPaths(
    root: JsonElement,
    path: String,
    out: MutableList<String>,
) {
    when {
        root.isJsonObject -> {
            out += path
            root.asJsonObject.entrySet().forEach { (k, child) ->
                collectContainerPaths(root = child, path = "$path.$k", out = out)
            }
        }

        root.isJsonArray -> {
            out += path
            root.asJsonArray.forEachIndexed { index, child ->
                collectContainerPaths(root = child, path = "$path[$index]", out = out)
            }
        }
    }
}

private fun formatJsonValue(element: JsonElement): String {
    if (element.isJsonNull) return "null"
    val primitive = element.asJsonPrimitive
    return when {
        primitive.isString -> "\"${primitive.asString}\""
        else -> primitive.toString()
    }
}

@Preview
@Composable
private fun InteractiveJsonViewerPreview() {
    var bits by remember { mutableStateOf("") }
    InteractiveJsonViewer(
        json = "{\"list\": [] }",
        onCopyAll = {},
        expandedBits = bits,
        onExpandedBitsChange = { bits = it },
    )
}
