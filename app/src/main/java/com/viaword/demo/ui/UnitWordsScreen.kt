package com.viaword.demo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.viaword.demo.R
import com.viaword.demo.data.WordUnit

@Composable
fun UnitWordsScreen(
    unit: WordUnit, action: Int?, enabled: Boolean, onBack: () -> Unit, onAction: () -> Unit,
    onPlay: (String) -> Unit, playingWord: String?, failedWord: String?,
    status: Int?, onRetry: (() -> Unit)?,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = pageMargin, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackButton(onBack)
                Text(unit.title, Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
            }

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = pageMargin),
            ) {
                item {
                    Column(Modifier.padding(top = 8.dp, bottom = 12.dp)) {
                        Text(stringResource(R.string.unit_word_list), style = MaterialTheme.typography.titleLarge)
                        SupportingText(stringResource(R.string.word_count, unit.entries.size))
                    }
                }
                items(unit.entries, key = { it.id }) { entry ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Text(entry.english, style = MaterialTheme.typography.titleMedium)
                        SupportingText(entry.ipa, Modifier.padding(top = 2.dp))
                        val meaning = entry.partOfSpeech?.let {
                            stringResource(R.string.meaning_with_part, entry.meaningZhHans, it)
                        } ?: entry.meaningZhHans
                        Text(meaning, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyLarge)
                        AudioButton(playingWord == entry.id, failedWord == entry.id) { onPlay(entry.id) }
                    }
                    HorizontalDivider()
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = pageMargin, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (action != null) Button(
                    onClick = onAction,
                    enabled = enabled,
                    shape = corner,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(action))
                }
                if (status != null) ProgressStatus(status, onRetry)
            }
        }
    }
}
