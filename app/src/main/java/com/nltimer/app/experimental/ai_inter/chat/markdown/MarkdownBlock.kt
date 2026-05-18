package com.nltimer.app.experimental.ai_inter.chat.markdown

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.jsoup.Jsoup

private val flavour by lazy { GFMFlavourDescriptor(makeHttpsAutoLinks = true, useSafeLinks = true) }
private val parser by lazy { MarkdownParser(flavour) }

private fun generateMarkdownHtml(content: String): String {
    val preprocessed = preProcess(content)
    val tree = parser.buildMarkdownTreeFromString(preprocessed)
    return HtmlGenerator(preprocessed, tree, flavour).generateHtml()
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Composable
fun MarkdownBlock(
    content: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    onClickCitation: (String) -> Unit = {},
) {
    var html by remember { mutableStateOf(generateMarkdownHtml(content)) }
    val updated by rememberUpdatedState(content)
    LaunchedEffect(Unit) {
        snapshotFlow { updated }
            .distinctUntilChanged()
            .mapLatest { generateMarkdownHtml(it) }
            .catch { it.printStackTrace() }
            .flowOn(Dispatchers.Default)
            .collect { html = it }
    }

    val document = remember(html) {
        runCatching { Jsoup.parse(html) }.getOrElse { Jsoup.parse("") }
    }

    ProvideTextStyle(style) {
        Column(modifier = modifier.padding(start = 4.dp)) {
            document.body().childNodes().fastForEach { node ->
                HtmlBodyNode(node = node, onClickCitation = onClickCitation)
            }
        }
    }
}
