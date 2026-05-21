package com.nltimer.feature.ai.chat.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.nltimer.feature.ai.chat.highlight.Highlighter
import com.nltimer.feature.ai.chat.highlight.HighlightTextColorPalette
import com.nltimer.feature.ai.chat.highlight.buildHighlightText

@Composable
fun HighlightCodeBlock(
    code: String,
    language: String,
    modifier: Modifier = Modifier,
    completeCodeBlock: Boolean = true,
) {
    val context = LocalContext.current
    val highlighter = remember { Highlighter(context) }
    val tokens by produceState<List<com.nltimer.feature.ai.chat.highlight.HighlightToken>>(emptyList(), code, language) {
        value = try {
            highlighter.highlight(code, language)
        } catch (_: Throwable) {
            emptyList()
        }
    }
    val colors = HighlightTextColorPalette.fromTheme()
    val highlighted = remember(tokens, colors) {
        if (tokens.isEmpty()) AnnotatedString(code)
        else buildAnnotatedString {
            tokens.forEach { buildHighlightText(it, colors) }
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(8.dp),
    ) {
        if (language.isNotBlank()) {
            Text(
                text = language,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Text(
                text = highlighted,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
        }
    }
}
