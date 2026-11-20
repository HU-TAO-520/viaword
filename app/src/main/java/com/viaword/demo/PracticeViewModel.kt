package com.viaword.demo

import android.app.Application
import android.database.SQLException
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.viaword.demo.data.AppCatalog
import com.viaword.demo.data.CurrentUnitPreferences
import com.viaword.demo.data.PracticeDatabase
import com.viaword.demo.data.PracticeStore
import com.viaword.demo.data.TaskSnapshot
import com.viaword.demo.data.WordUnit
import com.viaword.demo.practice.AnswerInput
import com.viaword.demo.practice.Move
import com.viaword.demo.practice.PracticeAction
import com.viaword.demo.practice.PracticeKind
import com.viaword.demo.practice.PracticeStep
import com.viaword.demo.practice.QuestionType
import com.viaword.demo.practice.Stage
import com.viaword.demo.ui.RootTab
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.launch

internal class PracticeViewModel(app: Application) : AndroidViewModel(app) {
    private val preferences = CurrentUnitPreferences(app, AppCatalog.units)
    private val store = PracticeStore(
        PracticeDatabase.get(app), preferences::readId, units = AppCatalog.units,
        eagerLearningEnabled = { preferences.eagerLearningEnabled },
    )
    private var answer by mutableStateOf(AnswerInput())
    private var refreshPending = false
    private var failedAction: Write? = null

    val words = AppCatalog.units.flatMap { it.entries }.associateBy { it.id }
    var tasks by mutableStateOf<TaskSnapshot?>(null)
        private set
    val practice get() = tasks?.practice?.copy(input = answer)
    var busy by mutableStateOf(false)
        private set
    var readFailed by mutableStateOf(false)
        private set
    var saveFailed by mutableStateOf(false)
        private set
    var libraryUpdated by mutableStateOf(false)
        private set

    var tab by mutableStateOf(RootTab.Today)
    var openUnitId by mutableStateOf<String?>(null)
    var settingsVisible by mutableStateOf(false)
    var eagerLearningEnabled by mutableStateOf(preferences.eagerLearningEnabled)
        private set
    var practiceVisible by mutableStateOf(false)
        private set

    fun setEagerLearning(enabled: Boolean) {
        preferences.eagerLearningEnabled = enabled
        eagerLearningEnabled = enabled
    }

    fun refresh() {
        if (busy) {
            refreshPending = true
        } else {
            run {
                apply(store.read())
            }
        }
    }

    fun checkDate() {
        if (tasks?.date != LocalDate.now()) refresh()
    }

    fun selectUnit(unit: WordUnit) {
        preferences.save(unit)
        showToday()
        refresh()
    }

    fun start(kind: PracticeKind, unitId: String) {
        write(Write(
            perform = { store.start(kind, unitId) },
            pending = { it.practice == null && it.unit.id == unitId },
            onApplied = { if (it.practice?.session?.kind == kind) practiceVisible = true },
        ))
    }

    fun resumePractice() {
        if (!busy && !readFailed && practice != null) practiceVisible = true
    }

    fun pausePractice() {
        practiceVisible = false
        refresh()
    }

    fun send(step: PracticeStep, action: PracticeAction) {
        val active = practice ?: return
        if (active.step != step) return
        if (action is Move) {
            val input = answer
            write(Write(
                perform = { store.move(step, action, input) },
                pending = { it.practice?.step == step },
                onApplied = { if (action == Move.CLOSE && it.practice == null) showToday() },
            ))
        } else if (active.session.stage == Stage.PROMPT && active.question.type != QuestionType.MEANING) {
            when (action) {
                is PracticeAction.Draft -> if (!busy && !saveFailed) answer = answer.copy(draft = action.input)
                is PracticeAction.AudioResult -> if (active.question.type == QuestionType.DICTATION) {
                    answer = answer.copy(heardAudio = answer.heardAudio || action.played, audioFailed = !action.played)
                }
                else -> Unit
            }
        }
    }

    fun retry() {
        val write = failedAction
        if (write == null) refresh()
        else if (!busy) run { recover(write, retry = true) }
    }

    fun dismissLibraryUpdate() {
        libraryUpdated = false
    }

    fun showToday() {
        tab = RootTab.Today
        openUnitId = null
        settingsVisible = false
    }

    private class Write(
        val perform: suspend () -> TaskSnapshot,
        val pending: (TaskSnapshot) -> Boolean,
        val onApplied: (TaskSnapshot) -> Unit,
    )

    private fun write(write: Write) {
        if (busy || readFailed || saveFailed || tasks == null) return
        run { perform(write) }
    }

    private suspend fun perform(write: Write) {
        failedAction = write
        try {
            val snapshot = write.perform()
            apply(snapshot)
            write.onApplied(snapshot)
            clearSaveError()
        } catch (error: Exception) {
            if (!storageError(error)) throw error
            saveFailed = true
            recover(write, retry = false)
        }
    }

    private suspend fun recover(write: Write, retry: Boolean) {
        val snapshot = store.read()
        apply(snapshot)
        if (!write.pending(snapshot)) {
            write.onApplied(snapshot)
            clearSaveError()
        } else if (retry) {
            perform(write)
        }
    }

    private fun clearSaveError() {
        failedAction = null
        saveFailed = false
    }

    private fun apply(snapshot: TaskSnapshot) {
        if (snapshot.practice?.step != tasks?.practice?.step) answer = AnswerInput()
        tasks = snapshot
        readFailed = false
        if (snapshot.libraryUpdated) {
            libraryUpdated = true
            showToday()
        }
        if (snapshot.practice == null) practiceVisible = false
    }

    private fun run(block: suspend () -> Unit) {
        busy = true
        viewModelScope.launch {
            try {
                block()
            } catch (error: Exception) {
                if (!storageError(error)) throw error
                readFailed = true
            } finally {
                busy = false
                if (refreshPending) {
                    refreshPending = false
                    refresh()
                }
            }
        }
    }

    private fun storageError(error: Exception): Boolean {
        if (error !is SQLException && error !is IOException && error !is IllegalStateException) return false
        Log.w("Viaword", "Unable to read or write practice progress", error)
        return true
    }
}
