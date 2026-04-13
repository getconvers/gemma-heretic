package com.gemmaheretic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gemmaheretic.app.ui.theme.CodeBlockBackground
import com.gemmaheretic.app.ui.theme.CodeBlockBackgroundLight

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true
) {
    val blocks = remember(text) { parseMarkdownBlocks(text) }

    SelectionContainer {
        Column(modifier = modifier) {
            blocks.forEach { block ->
                when (block) {
                    is MarkdownBlock.CodeBlock -> {
                        CodeBlockView(
                            code = block.code,
                            language = block.language,
                            isDarkTheme = isDarkTheme
                        )
                    }
                    is MarkdownBlock.TextBlock -> {
                        val annotated = remember(block.text) { parseInlineMarkdown(block.text) }
                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeBlockView(
    code: String,
    language: String,
    isDarkTheme: Boolean
) {
    val clipboardManager = LocalClipboardManager.current
    val bgColor = if (isDarkTheme) CodeBlockBackground else CodeBlockBackgroundLight

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
    ) {
        // Header with language and copy button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.ifBlank { "code" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = { clipboardManager.setText(AnnotatedString(code)) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Code content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                color = if (isDarkTheme) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

// --- Markdown Parsing ---

private sealed class MarkdownBlock {
    data class TextBlock(val text: String) : MarkdownBlock()
    data class CodeBlock(val code: String, val language: String) : MarkdownBlock()
}

private fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.split("\n")
    var inCodeBlock = false
    var codeLanguage = ""
    val codeBuilder = StringBuilder()
    val textBuilder = StringBuilder()

    for (line in lines) {
        if (line.trimStart().startsWith("```")) {
            if (inCodeBlock) {
                // End of code block
                blocks.add(MarkdownBlock.CodeBlock(codeBuilder.toString().trimEnd(), codeLanguage))
                codeBuilder.clear()
                inCodeBlock = false
                codeLanguage = ""
            } else {
                // Start of code block
                if (textBuilder.isNotEmpty()) {
                    blocks.add(MarkdownBlock.TextBlock(textBuilder.toString().trimEnd()))
                    textBuilder.clear()
                }
                codeLanguage = line.trimStart().removePrefix("```").trim()
                inCodeBlock = true
            }
        } else if (inCodeBlock) {
            if (codeBuilder.isNotEmpty()) codeBuilder.append("\n")
            codeBuilder.append(line)
        } else {
            if (textBuilder.isNotEmpty()) textBuilder.append("\n")
            textBuilder.append(line)
        }
    }

    // Handle remaining content
    if (inCodeBlock && codeBuilder.isNotEmpty()) {
        // Unclosed code block (still streaming)
        blocks.add(MarkdownBlock.CodeBlock(codeBuilder.toString().trimEnd(), codeLanguage))
    }
    if (textBuilder.isNotEmpty()) {
        blocks.add(MarkdownBlock.TextBlock(textBuilder.toString().trimEnd()))
    }

    return blocks
}

private fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val chars = text.toCharArray()
        val len = chars.size

        while (i < len) {
            when {
                // Bold **text**
                i + 1 < len && chars[i] == '*' && chars[i + 1] == '*' -> {
                    val end = text.indexOf("**", i + 2)
                    if (end > i + 2) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(chars[i])
                        i++
                    }
                }
                // Italic *text*
                chars[i] == '*' && (i == 0 || chars[i - 1] != '*') -> {
                    val end = text.indexOf('*', i + 1)
                    if (end > i + 1 && (end + 1 >= len || chars[end + 1] != '*')) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(chars[i])
                        i++
                    }
                }
                // Inline code `text`
                chars[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i + 1) {
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(chars[i])
                        i++
                    }
                }
                // Headings # text
                chars[i] == '#' && (i == 0 || chars[i - 1] == '\n') -> {
                    var level = 0
                    var j = i
                    while (j < len && chars[j] == '#') {
                        level++
                        j++
                    }
                    if (j < len && chars[j] == ' ') {
                        val lineEnd = text.indexOf('\n', j).let { if (it == -1) len else it }
                        val headerText = text.substring(j + 1, lineEnd)
                        val fontSize = when (level) {
                            1 -> 20.sp
                            2 -> 18.sp
                            else -> 16.sp
                        }
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = fontSize)) {
                            append(headerText)
                        }
                        i = lineEnd
                    } else {
                        append(chars[i])
                        i++
                    }
                }
                else -> {
                    append(chars[i])
                    i++
                }
            }
        }
    }
}
