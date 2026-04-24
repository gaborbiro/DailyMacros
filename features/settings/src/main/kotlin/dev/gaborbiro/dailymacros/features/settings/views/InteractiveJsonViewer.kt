package dev.gaborbiro.dailymacros.features.settings.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
    startExpanded: Boolean = false,
) {
    if (json.isBlank()) return

    val root = remember(json) { runCatching { JsonParser.parseString(json) }.getOrNull() } ?: return
    val containerPaths = remember(root) {
        buildList {
            collectContainerPaths(root = root, path = "root", out = this)
        }
    }
    val expandedState = remember(root) { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(root, startExpanded) {
        containerPaths.forEach { path -> expandedState[path] = startExpanded }
    }

    fun setAllExpanded(expanded: Boolean) {
        containerPaths.forEach { path -> expandedState[path] = expanded }
    }

    JsonViewerHeader(
        modifier = modifier.fillMaxWidth(),
        onCopyAll = onCopyAll,
        onCollapseAll = { setAllExpanded(false) },
        onExpandAll = { setAllExpanded(true) },
    )
    Spacer(modifier = Modifier.height(8.dp))
    SelectionContainer {
        Column {
            JsonNode(
                element = root,
                key = null,
                path = "root",
                indent = 0,
                expandedState = expandedState,
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
    expandedState: MutableMap<String, Boolean>,
) {
    when {
        element.isJsonObject -> JsonObjectNode(
            key = key,
            obj = element.asJsonObject,
            path = path,
            indent = indent,
            expandedState = expandedState,
        )

        element.isJsonArray -> JsonArrayNode(
            key = key,
            array = element.asJsonArray,
            path = path,
            indent = indent,
            expandedState = expandedState,
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
    expandedState: MutableMap<String, Boolean>,
) {
    val expanded = expandedState[path] == true
    JsonContainerHeader(
        indent = indent,
        key = key,
        openBracket = "{",
        closeBracket = "}",
        expanded = expanded,
        onToggle = { expandedState[path] = !expanded },
    )

    if (!expanded) return

    obj.entrySet().forEach { (childKey, childValue) ->
        JsonNode(
            element = childValue,
            key = childKey,
            path = "$path.$childKey",
            indent = indent + 1,
            expandedState = expandedState,
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
    expandedState: MutableMap<String, Boolean>,
) {
    val expanded = expandedState[path] == true
    JsonContainerHeader(
        indent = indent,
        key = key,
        openBracket = "[",
        closeBracket = "]",
        expanded = expanded,
        onToggle = { expandedState[path] = !expanded },
    )

    if (!expanded) return

    array.forEachIndexed { index, child ->
        JsonNode(
            element = child,
            key = null,
            path = "$path[$index]",
            indent = indent + 1,
            expandedState = expandedState,
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
        Text(
            text = " $closeBracket",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun JsonToggleButton(
    expanded: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        modifier = Modifier.size(32.dp),
        onClick = onClick,
        contentPadding = ButtonDefaults.TextButtonContentPadding,
    ) {
        Text(if (expanded) "-" else "+")
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
        }
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
    InteractiveJsonViewer(
        json = "{\"list\": [] }",
        onCopyAll = {},
    )
}
