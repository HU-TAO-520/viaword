package com.viaword.demo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.viaword.demo.R
import com.viaword.demo.data.AppCatalog

enum class RootTab { Today, Library }

@Composable
fun RootLayout(tab: RootTab, onTab: (RootTab) -> Unit, content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Column(Modifier.weight(1f)) { content() }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().selectableGroup()) {
                for (item in RootTab.entries) {
                    val selected = tab == item
                    val color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    Column(
                        modifier = Modifier.weight(1f)
                            .selectable(selected = selected, role = Role.Tab, onClick = { onTab(item) })
                            .heightIn(min = 48.dp).padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        TabIcon(item, color)
                        Text(
                            text = stringResource(if (item == RootTab.Today) R.string.today else R.string.library),
                            style = MaterialTheme.typography.bodyMedium,
                            color = color,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun TextbookHeader(title: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium)
    SupportingText(AppCatalog.title ?: stringResource(R.string.textbook), Modifier.padding(top = 4.dp))
    if (AppCatalog.title == null) SupportingText(stringResource(R.string.sample_library))
}

@Composable
internal fun SupportingText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun BackButton(onBack: () -> Unit) {
    val label = stringResource(R.string.back)
    IconButton(onClick = onBack, modifier = Modifier.size(48.dp).semantics {
        contentDescription = label
    }) {
        Chevron(back = true)
    }
}

@Composable
internal fun Chevron(back: Boolean = false) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(24.dp)) {
        val edge = if (back) 0.62f else 0.38f
        val tip = 1f - edge
        drawPath(
            path = Path().apply {
                moveTo(size.width * edge, size.height * 0.24f)
                lineTo(size.width * tip, size.height * 0.5f)
                lineTo(size.width * edge, size.height * 0.76f)
            },
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
private fun TabIcon(tab: RootTab, color: Color) {
    Canvas(Modifier.size(24.dp)) {
        val unit = size.width / 24
        val outline = Path().apply {
            if (tab == RootTab.Today) {
                moveTo(3 * unit, 11 * unit)
                lineTo(12 * unit, 3 * unit)
                lineTo(21 * unit, 11 * unit)
                lineTo(21 * unit, 21 * unit)
                lineTo(15 * unit, 21 * unit)
                lineTo(15 * unit, 14 * unit)
                lineTo(9 * unit, 14 * unit)
                lineTo(9 * unit, 21 * unit)
                lineTo(3 * unit, 21 * unit)
            } else {
                moveTo(2 * unit, 5 * unit)
                quadraticTo(7 * unit, 2 * unit, 12 * unit, 6 * unit)
                quadraticTo(17 * unit, 2 * unit, 22 * unit, 5 * unit)
                lineTo(22 * unit, 19 * unit)
                quadraticTo(17 * unit, 16 * unit, 12 * unit, 20 * unit)
                quadraticTo(7 * unit, 16 * unit, 2 * unit, 19 * unit)
                drawLine(color, Offset(12 * unit, 6 * unit), Offset(12 * unit, 20 * unit), 2.dp.toPx())
            }
            close()
        }
        drawPath(outline, color, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
    }
}
