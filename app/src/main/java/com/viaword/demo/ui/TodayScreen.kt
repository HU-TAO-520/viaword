package com.viaword.demo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.viaword.demo.R
import com.viaword.demo.data.WordUnit
import java.time.LocalDate

@Composable
fun TodayScreen(
    currentUnit: WordUnit?,
    availableNewCount: Int,
    dueQuestionCount: Int,
    activePractice: ActivePractice?,
    finishedTaskToday: Boolean,
    nextReviewDate: LocalDate?,
    onOpenUnit: () -> Unit,
    onStartStudy: (() -> Unit)? = null,
    onStartReview: (() -> Unit)? = null,
    onResume: (() -> Unit)? = null,
    status: Int? = null,
    onRetry: (() -> Unit)? = null,
) {
    val state = todayState(availableNewCount, dueQuestionCount, activePractice, finishedTaskToday)
    val words = stringResource(R.string.word_count, availableNewCount)
    val questions = stringResource(R.string.question_count, dueQuestionCount)
    val nextReview = nextReviewDate?.let {
        stringResource(R.string.next_review, it.year, it.monthValue, it.dayOfMonth)
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = pageMargin, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column { TextbookHeader(stringResource(R.string.app_name)) }
        Text(
            stringResource(R.string.today_heading),
            Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        if (status != null) {
            ProgressStatus(status, onRetry)
        } else when (state) {
            TodayState.Studying, TodayState.Reviewing -> {
                requireNotNull(activePractice)
                val studying = state == TodayState.Studying
                PracticeCard(
                    title = if (studying) R.string.studying else R.string.reviewing,
                    detail = stringResource(
                        if (studying) R.string.study_progress else R.string.review_progress,
                        activePractice.completed, activePractice.total,
                    ),
                    action = when {
                        activePractice.summary -> R.string.view_summary
                        studying -> R.string.continue_study
                        else -> R.string.continue_review
                    },
                    onAction = onResume,
                    subtitle = activePractice.unitTitle ?: if (activePractice.retry) {
                        stringResource(R.string.retry_in_progress)
                    } else null,
                )
                if (studying && dueQuestionCount > 0) {
                    ReadOnlyTask(R.string.due_review, questions)
                }
                if (!studying && availableNewCount > 0) {
                    ReadOnlyTask(R.string.new_words, words)
                }
            }
            TodayState.DueReview -> {
                PracticeCard(R.string.due_review, questions, R.string.start_review, onStartReview)
                if (availableNewCount > 0) {
                    OutlinedButton(
                        onClick = { onStartStudy?.invoke() },
                        enabled = onStartStudy != null,
                        shape = corner,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.new_words))
                            Text(words, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            TodayState.NewWords -> {
                PracticeCard(R.string.new_words, words, R.string.start_study, onStartStudy)
                SupportingText(stringResource(R.string.no_due_review))
            }
            TodayState.Finished -> PracticeCard(R.string.finished_today, nextReview)
            TodayState.Idle -> PracticeCard(R.string.no_practice, nextReview)
        }
        if (currentUnit != null) Surface(
            shape = corner,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpenUnit)
                    .heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(stringResource(R.string.current_unit), style = MaterialTheme.typography.titleMedium)
                    SupportingText(currentUnit.title)
                }
                Chevron()
            }
        }
    }
}

@Composable
private fun PracticeCard(
    title: Int, detail: String?, action: Int? = null, onAction: (() -> Unit)? = null, subtitle: String? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = corner,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) SupportingText(subtitle)
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodyLarge)
            if (action != null) {
                Button(
                    onClick = { onAction?.invoke() },
                    enabled = onAction != null,
                    shape = corner,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(action))
                }
            }
        }
    }
}

@Composable
internal fun ProgressStatus(message: Int, onRetry: (() -> Unit)?) {
    SupportingText(stringResource(message))
    if (onRetry != null) {
        TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun ReadOnlyTask(title: Int, detail: String) {
    Column {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        SupportingText(detail)
    }
}
