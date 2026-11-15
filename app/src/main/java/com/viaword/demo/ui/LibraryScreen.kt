package com.viaword.demo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.viaword.demo.R
import com.viaword.demo.data.WordUnit

@Composable
fun LibraryScreen(units: List<WordUnit>, currentUnitId: String, onOpenUnit: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = pageMargin, vertical = 20.dp),
    ) {
        TextbookHeader(stringResource(R.string.library))
        HorizontalDivider(Modifier.padding(top = 24.dp))
        for (unit in units) {
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = { onOpenUnit(unit.id) })
                    .heightIn(min = 48.dp).padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(unit.title, style = MaterialTheme.typography.titleLarge)
                    SupportingText(stringResource(R.string.word_count, unit.entries.size))
                    if (unit.id == currentUnitId) {
                        Text(
                            stringResource(R.string.current_unit),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Chevron()
            }
            HorizontalDivider()
        }
    }
}
