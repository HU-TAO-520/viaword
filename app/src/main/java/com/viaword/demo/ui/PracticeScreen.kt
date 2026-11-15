package com.viaword.demo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.viaword.demo.R
import com.viaword.demo.data.WordEntry
import com.viaword.demo.data.needsRetry
import com.viaword.demo.data.reviewSummary
import com.viaword.demo.practice.Move
import com.viaword.demo.practice.Outcome
import com.viaword.demo.practice.Phase
import com.viaword.demo.practice.Practice
import com.viaword.demo.practice.PracticeAction
import com.viaword.demo.practice.PracticeKind
import com.viaword.demo.practice.QuestionType
import com.viaword.demo.practice.Stage
import com.viaword.demo.practice.grade
import java.time.LocalDate

@Composable
internal fun PracticeScreen(
    practice: Practice,
    word: WordEntry?,
    today: LocalDate,
    playing: Boolean,
    audioFailed: Boolean,
    enabled: Boolean,
    error: Int?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onAction: (PracticeAction) -> Unit,
    onPlay: () -> Unit,
) {
    val stage = practice.session.stage
    val learning = practice.session.kind == PracticeKind.LEARN
    val title = when (practice.session.phase) {
        Phase.LEARN -> R.string.new_words
        Phase.FIRST -> R.string.review
        Phase.RETRY -> R.string.retry_questions
        Phase.SUMMARY -> if (learning) R.string.study_summary else R.string.practice_summary
    }
    val summary = stage == Stage.SUMMARY
    val total = practice.total
    val lastWord = stage == Stage.CARD && practice.index == total - 1
    val position = if (learning) R.string.word_position else R.string.question_position

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = pageMargin),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackButton(onBack)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
                    if (!summary) SupportingText(stringResource(position, practice.index + 1, total))
                }
            }
            if (!summary) {
                LinearProgressIndicator(
                    progress = { (practice.index + 1f) / total },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = pageMargin, vertical = 8.dp),
                )
            }
            key(practice.step) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                        .padding(horizontal = pageMargin, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (error != null) ProgressStatus(error, onRetry)
                    when (stage) {
                        Stage.CARD -> {
                            val entry = requireNotNull(word)
                            WordTitle(entry.english)
                            SupportingText(entry.ipa)
                            AudioButton(playing, audioFailed, onPlay)
                            WordMeaning(entry, separatePart = true)
                        }
                        Stage.PROMPT, Stage.REVEALED -> QuestionContent(
                            practice, requireNotNull(word), playing, audioFailed, enabled, onAction, onPlay,
                        )
                        Stage.FEEDBACK -> AnswerFeedback(practice, requireNotNull(word), playing, audioFailed, onPlay)
                        Stage.SUMMARY -> if (learning) {
                            WordTitle(stringResource(R.string.learned_count, practice.completed))
                            if (practice.completed > 0) SupportingText(stringResource(R.string.review_starts_tomorrow))
                            if (practice.session.endedByDailyLimit) SupportingText(stringResource(R.string.daily_limit))
                        } else {
                            val results = reviewSummary(practice.items, today)
                            SummaryLine(R.string.first_correct, R.string.question_count, results.correct)
                            SummaryLine(
                                if (results.dueTomorrow) R.string.review_tomorrow else R.string.missed_answers,
                                R.string.question_count, results.missed,
                            )
                            if (!results.dueTomorrow) SupportingText(stringResource(R.string.review_scheduled))
                            if (results.skipped > 0) {
                                SummaryLine(R.string.audio_skipped, R.string.question_count, results.skipped)
                            }
                        }
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = pageMargin, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                when (stage) {
                    Stage.CARD -> {
                        PrimaryAction(if (lastWord) R.string.finish_learning else R.string.next_word, enabled) {
                            onAction(Move.NEXT_WORD)
                        }
                    }
                    Stage.PROMPT -> if (practice.question.type == QuestionType.MEANING) {
                        PrimaryAction(R.string.reveal_answer, enabled) { onAction(Move.REVEAL) }
                    } else {
                        ActionPair(
                            R.string.unknown_answer, R.string.submit_answer,
                            enabled && practice.canAnswer, enabled && practice.canSubmit,
                            onSecondary = { onAction(Move.UNKNOWN) },
                            onPrimary = { onAction(Move.SUBMIT) },
                        )
                    }
                    Stage.REVEALED -> ActionPair(
                        R.string.forgot, R.string.remembered, enabled, enabled,
                        onSecondary = { onAction(Move.FORGOT) },
                        onPrimary = { onAction(Move.REMEMBERED) },
                    )
                    Stage.FEEDBACK -> PrimaryAction(R.string.continue_next, enabled) { onAction(Move.CONTINUE) }
                    Stage.SUMMARY -> PrimaryAction(R.string.return_today, enabled, onFinish)
                }
            }
        }
    }
}

@Composable
private fun QuestionContent(
    practice: Practice, word: WordEntry, playing: Boolean, audioFailed: Boolean, enabled: Boolean,
    onAction: (PracticeAction) -> Unit, onPlay: () -> Unit,
) {
    val type = practice.question.type
    when (type) {
        QuestionType.MEANING -> {
            SupportingText(stringResource(R.string.meaning_prompt))
            WordTitle(word.english)
            if (practice.session.stage == Stage.REVEALED) {
                Text(word.meaningZhHans, style = MaterialTheme.typography.titleLarge)
                SupportingText(word.ipa)
                AudioButton(playing, audioFailed, onPlay)
                SupportingText(stringResource(R.string.recall_hint))
            }
        }
        QuestionType.SPELLING -> {
            SupportingText(stringResource(R.string.spelling_prompt))
            WordMeaning(word)
        }
        QuestionType.DICTATION -> {
            SupportingText(stringResource(R.string.dictation_prompt))
            if (practice.input.audioFailed) {
                Text(stringResource(R.string.audio_failed))
                OutlinedButton(onClick = onPlay, shape = corner, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.retry))
                }
                TextButton(
                    onClick = { onAction(Move.SKIP_AUDIO) },
                    enabled = enabled,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.skip_audio))
                }
            } else {
                AudioButton(playing || practice.input.heardAudio, failed = false, onPlay = onPlay)
            }
        }
    }
    if (type != QuestionType.MEANING) {
        OutlinedTextField(
            value = practice.input.draft,
            onValueChange = { onAction(PracticeAction.Draft(it)) },
            readOnly = !enabled,
            label = { Text(stringResource(R.string.english_input)) },
            singleLine = true,
            shape = corner,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onAction(Move.SUBMIT) }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AnswerFeedback(
    practice: Practice, word: WordEntry, playing: Boolean, audioFailed: Boolean, onPlay: () -> Unit,
) {
    val input = practice.session.feedbackInput
    val usedAlternative = input != null && grade(practice.question, word, input).usedAlternative
    val outcome = practice.feedbackOutcome
    WordTitle(stringResource(when (outcome) {
        Outcome.CORRECT -> R.string.answer_correct
        Outcome.INCORRECT -> R.string.answer_incorrect
        else -> R.string.answer_unknown
    }))
    if (input != null) {
        Column {
            SupportingText(stringResource(R.string.your_answer))
            Text(input, style = MaterialTheme.typography.titleLarge)
        }
        if (usedAlternative || outcome == Outcome.INCORRECT) {
            SupportingText(stringResource(if (usedAlternative) R.string.lesson_entry else R.string.correct_answer))
        }
    }
    if (practice.session.phase == Phase.FIRST && outcome.needsRetry) {
        SupportingText(stringResource(R.string.retry_later_in_round))
    }
    WordTitle(word.english)
    SupportingText(word.ipa)
    WordMeaning(word)
    AudioButton(playing, audioFailed, onPlay)
}

@Composable
internal fun AudioButton(playing: Boolean, failed: Boolean, onPlay: () -> Unit) {
    val label = when {
        failed -> R.string.audio_failed_retry
        playing -> R.string.replay_audio
        else -> R.string.play_audio
    }
    OutlinedButton(onClick = onPlay, shape = corner, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(stringResource(label))
    }
}

@Composable
private fun WordMeaning(word: WordEntry, separatePart: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (separatePart) word.partOfSpeech?.let { SupportingText(it) }
        Text(word.meaningZhHans, style = MaterialTheme.typography.titleLarge)
        if (!separatePart) word.partOfSpeech?.let { SupportingText(it) }
    }
}

@Composable
private fun WordTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineLarge)
}

@Composable
private fun SummaryLine(label: Int, format: Int, count: Int) {
    Surface(shape = corner, color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            SupportingText(stringResource(label))
            Text(stringResource(format, count), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun PrimaryAction(label: Int, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled, shape = corner) {
        Text(stringResource(label))
    }
}

@Composable
private fun ActionPair(
    secondary: Int, primary: Int, secondaryEnabled: Boolean = true, primaryEnabled: Boolean = true,
    onSecondary: () -> Unit, onPrimary: () -> Unit,
) {
    OutlinedButton(
        onSecondary, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = secondaryEnabled, shape = corner,
    ) {
        Text(stringResource(secondary))
    }
    PrimaryAction(primary, primaryEnabled, onPrimary)
}

@Composable
internal fun ExitPracticeDialog(onDismiss: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exit_title)) },
        text = { Text(stringResource(R.string.exit_message)) },
        confirmButton = {
            TextButton(onClick = onExit, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.exit_practice))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.keep_practicing))
            }
        },
    )
}
