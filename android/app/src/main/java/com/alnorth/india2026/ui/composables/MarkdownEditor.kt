package com.alnorth.india2026.ui.composables

import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin

/**
 * WYSIWYM (What You See Is What You Mean) Markdown Editor
 *
 * Features:
 * - Formatting toolbar with common markdown operations
 * - Live preview toggle to see rendered markdown
 * - Syntax helpers for bold, italic, headers, links, lists, etc.
 */
@Composable
fun MarkdownEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Write your content in markdown...",
    label: String = "Content (Markdown)"
) {
    var textFieldValue by remember(value) {
        mutableStateOf(TextFieldValue(value))
    }
    var showPreview by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Markwon instance for rendering markdown
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .build()
    }

    Column(modifier = modifier) {
        // Toolbar
        MarkdownToolbar(
            showPreview = showPreview,
            onTogglePreview = { showPreview = !showPreview },
            onFormatAction = { action ->
                val result = applyFormatting(textFieldValue, action)
                textFieldValue = result
                onValueChange(result.text)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Editor or Preview
        if (showPreview) {
            MarkdownPreview(
                markdown = textFieldValue.text,
                markwon = markwon,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 400.dp)
            )
        } else {
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    onValueChange(newValue.text)
                },
                label = { Text(label) },
                placeholder = { Text(placeholder) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                minLines = 8,
                maxLines = Int.MAX_VALUE
            )
        }

        // Markdown hints
        if (!showPreview) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tip: Use **bold**, *italic*, # headings, - lists",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun MarkdownToolbar(
    showPreview: Boolean,
    onTogglePreview: () -> Unit,
    onFormatAction: (FormatAction) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preview toggle
            FilledIconToggleButton(
                checked = showPreview,
                onCheckedChange = { onTogglePreview() }
            ) {
                Icon(
                    imageVector = if (showPreview) Icons.Filled.Edit else Icons.Filled.Visibility,
                    contentDescription = if (showPreview) "Edit" else "Preview"
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 4.dp)
            )

            // Formatting buttons (only enabled when not in preview)
            FormatButton(
                icon = Icons.Filled.FormatBold,
                contentDescription = "Bold",
                enabled = !showPreview,
                onClick = { onFormatAction(FormatAction.Bold) }
            )
            FormatButton(
                icon = Icons.Filled.FormatItalic,
                contentDescription = "Italic",
                enabled = !showPreview,
                onClick = { onFormatAction(FormatAction.Italic) }
            )
            FormatButton(
                icon = Icons.Filled.FormatStrikethrough,
                contentDescription = "Strikethrough",
                enabled = !showPreview,
                onClick = { onFormatAction(FormatAction.Strikethrough) }
            )

            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 4.dp)
            )

            FormatButton(
                icon = Icons.Filled.Title,
                contentDescription = "Heading",
                enabled = !showPreview,
                onClick = { onFormatAction(FormatAction.Heading) }
            )
            FormatButton(
                icon = Icons.Filled.FormatQuote,
                contentDescription = "Quote",
                enabled = !showPreview,
                onClick = { onFormatAction(FormatAction.Quote) }
            )
            FormatButton(
                icon = Icons.Filled.Code,
                contentDescription = "Code",
                enabled = !showPreview,
                onClick = { onFormatAction(FormatAction.Code) }
            )

            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 4.dp)
            )

            FormatButton(
                icon = Icons.Filled.FormatListBulleted,
                contentDescription = "Bullet List",
                enabled = !showPreview,
                onClick = { onFormatAction(FormatAction.BulletList) }
            )
            FormatButton(
                icon = Icons.Filled.FormatListNumbered,
                contentDescription = "Numbered List",
                enabled = !showPreview,
                onClick = { onFormatAction(FormatAction.NumberedList) }
            )

            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 4.dp)
            )

            FormatButton(
                icon = Icons.Filled.Link,
                contentDescription = "Link",
                enabled = !showPreview,
                onClick = { onFormatAction(FormatAction.Link) }
            )
            FormatButton(
                icon = Icons.Filled.HorizontalRule,
                contentDescription = "Horizontal Rule",
                enabled = !showPreview,
                onClick = { onFormatAction(FormatAction.HorizontalRule) }
            )
        }
    }
}

@Composable
private fun FormatButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MarkdownPreview(
    markdown: String,
    markwon: Markwon,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            ),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Preview header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Preview",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

            // Rendered markdown
            if (markdown.isBlank()) {
                Text(
                    text = "Nothing to preview",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                SelectionContainer {
                    AndroidView(
                        factory = { context ->
                            TextView(context).apply {
                                setTextColor(textColor)
                                setLinkTextColor(linkColor)
                                textSize = 16f
                                setPadding(0, 0, 0, 0)
                            }
                        },
                        update = { textView ->
                            markwon.setMarkdown(textView, markdown)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

/**
 * Formatting actions available in the toolbar
 */
private enum class FormatAction {
    Bold,
    Italic,
    Strikethrough,
    Heading,
    Quote,
    Code,
    BulletList,
    NumberedList,
    Link,
    HorizontalRule
}

/**
 * Applies markdown formatting to the text field value
 */
private fun applyFormatting(
    textFieldValue: TextFieldValue,
    action: FormatAction
): TextFieldValue {
    val text = textFieldValue.text
    val selection = textFieldValue.selection
    val selectedText = if (selection.collapsed) "" else text.substring(selection.min, selection.max)

    return when (action) {
        FormatAction.Bold -> wrapSelection(textFieldValue, "**", "**", "bold text")
        FormatAction.Italic -> wrapSelection(textFieldValue, "*", "*", "italic text")
        FormatAction.Strikethrough -> wrapSelection(textFieldValue, "~~", "~~", "strikethrough text")
        FormatAction.Code -> {
            if (selectedText.contains("\n")) {
                wrapSelection(textFieldValue, "```\n", "\n```", "code")
            } else {
                wrapSelection(textFieldValue, "`", "`", "code")
            }
        }
        FormatAction.Heading -> insertAtLineStart(textFieldValue, "## ")
        FormatAction.Quote -> insertAtLineStart(textFieldValue, "> ")
        FormatAction.BulletList -> insertAtLineStart(textFieldValue, "- ")
        FormatAction.NumberedList -> insertAtLineStart(textFieldValue, "1. ")
        FormatAction.Link -> {
            if (selectedText.isNotEmpty()) {
                wrapSelection(textFieldValue, "[", "](url)", "")
            } else {
                insertText(textFieldValue, "[link text](url)")
            }
        }
        FormatAction.HorizontalRule -> {
            val lineStart = findLineStart(text, selection.min)
            val prefix = if (lineStart == selection.min || text.getOrNull(selection.min - 1) == '\n') "" else "\n"
            insertText(textFieldValue, "${prefix}---\n")
        }
    }
}

/**
 * Wraps the selected text with prefix and suffix, or inserts placeholder if nothing selected
 */
private fun wrapSelection(
    textFieldValue: TextFieldValue,
    prefix: String,
    suffix: String,
    placeholder: String
): TextFieldValue {
    val text = textFieldValue.text
    val selection = textFieldValue.selection

    return if (selection.collapsed) {
        // No selection - insert placeholder with formatting
        val newText = text.substring(0, selection.min) +
                prefix + placeholder + suffix +
                text.substring(selection.min)
        val cursorPos = selection.min + prefix.length
        TextFieldValue(
            text = newText,
            selection = TextRange(cursorPos, cursorPos + placeholder.length)
        )
    } else {
        // Wrap selected text
        val selectedText = text.substring(selection.min, selection.max)
        val newText = text.substring(0, selection.min) +
                prefix + selectedText + suffix +
                text.substring(selection.max)
        TextFieldValue(
            text = newText,
            selection = TextRange(
                selection.min + prefix.length,
                selection.max + prefix.length
            )
        )
    }
}

/**
 * Inserts text at the start of the current line
 */
private fun insertAtLineStart(
    textFieldValue: TextFieldValue,
    insertion: String
): TextFieldValue {
    val text = textFieldValue.text
    val selection = textFieldValue.selection
    val lineStart = findLineStart(text, selection.min)

    val newText = text.substring(0, lineStart) + insertion + text.substring(lineStart)
    val newCursorPos = selection.min + insertion.length

    return TextFieldValue(
        text = newText,
        selection = TextRange(newCursorPos, newCursorPos + (selection.max - selection.min))
    )
}

/**
 * Inserts text at cursor position
 */
private fun insertText(
    textFieldValue: TextFieldValue,
    insertion: String
): TextFieldValue {
    val text = textFieldValue.text
    val selection = textFieldValue.selection

    val newText = text.substring(0, selection.min) + insertion + text.substring(selection.max)
    val newCursorPos = selection.min + insertion.length

    return TextFieldValue(
        text = newText,
        selection = TextRange(newCursorPos)
    )
}

/**
 * Finds the start of the line containing the given position
 */
private fun findLineStart(text: String, position: Int): Int {
    var pos = position - 1
    while (pos >= 0 && text[pos] != '\n') {
        pos--
    }
    return pos + 1
}
