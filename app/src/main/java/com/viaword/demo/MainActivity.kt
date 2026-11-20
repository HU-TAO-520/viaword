package com.viaword.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import com.viaword.demo.data.AppCatalog
import com.viaword.demo.practice.Move
import com.viaword.demo.practice.Phase
import com.viaword.demo.practice.PracticeAction
import com.viaword.demo.practice.PracticeKind
import com.viaword.demo.practice.QuestionType
import com.viaword.demo.practice.Stage
import com.viaword.demo.ui.ActivePractice
import com.viaword.demo.ui.ExitPracticeDialog
import com.viaword.demo.ui.LibraryScreen
import com.viaword.demo.ui.PracticeScreen
import com.viaword.demo.ui.PracticeType
import com.viaword.demo.ui.RootLayout
import com.viaword.demo.ui.RootTab
import com.viaword.demo.ui.SettingsScreen
import com.viaword.demo.ui.TodayScreen
import com.viaword.demo.ui.UnitWordsScreen
import com.viaword.demo.ui.ViawordTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val audio by lazy { SampleAudio(this) }
    private val model by lazy { ViewModelProvider(this)[PracticeViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lightBars = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.WHITE)
        enableEdgeToEdge(statusBarStyle = lightBars, navigationBarStyle = lightBars)
        setContent { ViawordTheme { ViawordApp(model, audio) } }
    }

    override fun onResume() {
        super.onResume()
        model.refresh()
    }

    override fun onPause() {
        audio.stop()
        super.onPause()
    }

    override fun onDestroy() {
        audio.stop()
        super.onDestroy()
    }
}

@Composable
private fun ViawordApp(model: PracticeViewModel, audio: SampleAudio) {
    val tasks = model.tasks
    val practice = model.practice
    val currentUnit = tasks?.unit
    val openUnit = AppCatalog.units.firstOrNull { it.id == model.openUnitId }
    val ready = tasks != null && !model.busy && !model.readFailed && !model.saveFailed
    val home = !model.practiceVisible && !model.settingsVisible && model.tab == RootTab.Today && openUnit == null
    val error = when {
        model.saveFailed -> R.string.progress_save_failed
        model.readFailed -> R.string.progress_read_failed
        else -> null
    }

    LaunchedEffect(home) {
        if (home) model.refresh()
    }
    val watchDate = home || !model.practiceVisible && openUnit != null ||
        model.practiceVisible && practice?.session?.stage == Stage.SUMMARY
    LaunchedEffect(watchDate) {
        while (watchDate) {
            delay(30_000)
            model.checkDate()
        }
    }
    if (model.libraryUpdated) {
        AlertDialog(
            onDismissRequest = model::dismissLibraryUpdate,
            text = { Text(stringResource(R.string.library_updated)) },
            confirmButton = {
                TextButton(onClick = model::dismissLibraryUpdate) { Text(stringResource(android.R.string.ok)) }
            },
        )
    }

    fun closeList() {
        audio.stop()
        model.openUnitId = null
    }

    if (model.settingsVisible) {
        BackHandler(onBack = model::showToday)
        SettingsScreen(model.eagerLearningEnabled, model::setEagerLearning, model::showToday)
        return
    }

    if (model.practiceVisible && practice != null && tasks != null) {
        val step = practice.step
        var confirmExit by rememberSaveable(step.sessionId) { mutableStateOf(false) }
        val keyboard = LocalSoftwareKeyboardController.current
        val focus = LocalFocusManager.current
        val summary = step.stage == Stage.SUMMARY
        fun finish() {
            if (ready) model.send(step, Move.CLOSE)
        }
        val back = {
            if (summary) {
                finish()
            } else if (!model.busy) {
                focus.clearFocus()
                keyboard?.hide()
                audio.stop()
                confirmExit = true
            }
        }
        LaunchedEffect(step) {
            audio.stop()
            focus.clearFocus()
            keyboard?.hide()
        }
        BackHandler(enabled = !confirmExit, onBack = back)
        val wordId = if (summary) null else practice.item.wordId
        PracticeScreen(
            practice = practice,
            word = wordId?.let(model.words::getValue),
            today = tasks.date,
            playing = wordId != null && audio.playingWord == wordId,
            audioFailed = wordId != null && audio.failedWord == wordId,
            enabled = ready,
            error = error,
            onRetry = model::retry,
            onBack = back,
            onFinish = ::finish,
            onAction = { action -> if (ready && !confirmExit) model.send(step, action) },
            onPlay = {
                if (!confirmExit && model.practice?.step == step && wordId != null) {
                    val dictation = step.stage == Stage.PROMPT && practice.question.type == QuestionType.DICTATION
                    audio.play(wordId) { played ->
                        if (dictation) model.send(step, PracticeAction.AudioResult(played))
                    }
                }
            },
        )
        if (confirmExit) {
            ExitPracticeDialog(onDismiss = { confirmExit = false }, onExit = {
                audio.stop()
                confirmExit = false
                model.pausePractice()
            })
        }
        return
    }

    BackHandler(openUnit != null || model.tab == RootTab.Library) {
        if (openUnit != null) closeList()
        else model.showToday()
    }

    if (openUnit != null) {
        val isCurrent = openUnit.id == currentUnit?.id
        val action = when {
            currentUnit == null -> null
            !isCurrent -> R.string.set_current_unit
            practice != null -> R.string.go_to_today
            tasks.extraNewCount > 0 -> R.string.continue_new_words
            else -> R.string.study_unit
        }
        val unavailable = when {
            !isCurrent || practice != null -> null
            tasks.remainingNewWords == 0 && !tasks.extraLearningAllowed -> R.string.daily_limit
            tasks.availableNewCount == 0 -> R.string.unit_finished
            else -> null
        }
        UnitWordsScreen(
            unit = openUnit,
            action = action,
            enabled = ready && unavailable == null,
            onBack = ::closeList,
            onAction = {
                audio.stop()
                when (action) {
                    R.string.set_current_unit -> model.selectUnit(openUnit)
                    R.string.go_to_today -> model.showToday()
                    R.string.study_unit, R.string.continue_new_words -> model.start(PracticeKind.LEARN, openUnit.id)
                }
            },
            onPlay = { id -> audio.play(id) {} },
            playingWord = audio.playingWord,
            failedWord = audio.failedWord,
            status = error ?: unavailable ?: R.string.progress_loading.takeIf { tasks == null },
            onRetry = if (error != null && !model.busy) model::retry else null,
        )
    } else {
        RootLayout(model.tab, onTab = { model.tab = it }) {
            when (model.tab) {
                RootTab.Today -> TodayScreen(
                    currentUnit = currentUnit,
                    availableNewCount = tasks?.regularNewCount ?: 0,
                    extraNewCount = tasks?.extraNewCount ?: 0,
                    dueQuestionCount = tasks?.dueQuestionCount ?: 0,
                    activePractice = practice?.let {
                        val learning = it.session.kind == PracticeKind.LEARN
                        ActivePractice(
                            type = if (learning) PracticeType.Study else PracticeType.Review,
                            completed = it.completed,
                            total = it.items.size,
                            unitTitle = AppCatalog.units.firstOrNull { unit -> unit.id == it.session.unitId }?.title,
                            summary = it.session.phase == Phase.SUMMARY,
                            retry = it.session.phase == Phase.RETRY,
                        )
                    },
                    finishedTaskToday = tasks?.finishedTaskToday == true,
                    nextReviewDate = tasks?.nextReviewDate,
                    onOpenUnit = { model.openUnitId = currentUnit?.id },
                    onOpenSettings = { model.settingsVisible = true },
                    onStartStudy = if (ready) ({ model.start(PracticeKind.LEARN, requireNotNull(currentUnit).id) }) else null,
                    onStartReview = if (ready) ({ model.start(PracticeKind.REVIEW, requireNotNull(currentUnit).id) }) else null,
                    onResume = if (ready) model::resumePractice else null,
                    status = error ?: R.string.progress_loading.takeIf { tasks == null },
                    onRetry = if (error != null && !model.busy) model::retry else null,
                )
                RootTab.Library -> LibraryScreen(
                    units = AppCatalog.units,
                    currentUnitId = currentUnit?.id.orEmpty(),
                    onOpenUnit = { model.openUnitId = it },
                )
            }
        }
    }
}
