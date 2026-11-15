package com.viaword.demo.data

import androidx.room.withTransaction
import com.viaword.demo.practice.AnswerInput
import com.viaword.demo.practice.Move
import com.viaword.demo.practice.Outcome
import com.viaword.demo.practice.Phase
import com.viaword.demo.practice.Practice
import com.viaword.demo.practice.PracticeKind
import com.viaword.demo.practice.PracticeStep
import com.viaword.demo.practice.QuestionType
import com.viaword.demo.practice.Stage
import com.viaword.demo.practice.resolveAnswer
import java.time.LocalDate

internal data class TaskSnapshot(
    val unit: WordUnit,
    val date: LocalDate,
    val availableNewCount: Int,
    val remainingNewWords: Int,
    val dueQuestionCount: Int,
    val practice: Practice?,
    val finishedTaskToday: Boolean,
    val nextReviewDate: LocalDate?,
    val libraryUpdated: Boolean,
)

internal class PracticeStore(
    private val db: PracticeDatabase,
    private val currentUnitId: () -> String,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val units: List<WordUnit> = sampleUnits,
) {
    private val dao = db.practiceDao()
    private val words = units.flatMap { it.entries }.associateBy { it.id }
    private val positions = words.keys.withIndex().associate { (index, id) -> id to index }

    suspend fun read(): TaskSnapshot = transact { _, _, _ -> }

    suspend fun start(kind: PracticeKind, unitId: String): TaskSnapshot = transact { date, unit, active ->
        if (active != null || unit.id != unitId) return@transact
        val day = date.toEpochDay()
        val session: PracticeSession
        val entries: List<Pair<String, QuestionType?>>
        if (kind == PracticeKind.LEARN) {
            val learned = dao.learnedWords()
            entries = newWords(unit, learned, remainingNewWords(learned, day)).map { it.id to null }
            session = PracticeSession(kind = kind, unitId = unitId, phase = Phase.LEARN, stage = Stage.CARD)
        } else {
            entries = reviewQueue(dueReviews(dao.schedules(), positions, day)).map { it.wordId to it.questionType }
            session = PracticeSession(kind = kind, phase = Phase.FIRST, stage = Stage.PROMPT)
        }
        if (entries.isEmpty()) return@transact
        val id = dao.insertSession(session)
        dao.insertItems(entries.mapIndexed { ordinal, (wordId, type) -> PracticeItem(id, ordinal, wordId, type) })
    }

    suspend fun move(step: PracticeStep, move: Move, input: AnswerInput = AnswerInput()): TaskSnapshot =
        transact { date, _, active ->
            if (active == null || active.step != step) return@transact
            val session = active.session
            when (session.stage) {
                Stage.CARD -> if (move == Move.NEXT_WORD) confirmWord(active, date)
                Stage.SUMMARY -> if (move == Move.CLOSE) {
                    dao.updateSession(session.copy(openSlot = null, feedbackInput = null))
                }
                Stage.FEEDBACK -> if (move == Move.CONTINUE) advanceReview(active, date)
                Stage.PROMPT, Stage.REVEALED -> {
                    val question = active.question
                    if (move == Move.REVEAL && session.stage == Stage.PROMPT && question.type == QuestionType.MEANING) {
                        dao.updateSession(session.copy(stage = Stage.REVEALED))
                    } else {
                        val word = words.getValue(question.wordId)
                        val outcome = resolveAnswer(session.stage, question, word, move, input) ?: return@transact
                        recordReview(active, date, outcome, input.draft.takeIf { move == Move.SUBMIT })
                    }
                }
            }
        }

    private suspend fun transact(
        action: suspend (LocalDate, WordUnit, Practice?) -> Unit,
    ): TaskSnapshot = db.withTransaction {
        val date = today()
        val day = date.toEpochDay()
        val id = currentUnitId()
        val unit = units.firstOrNull { it.id == id } ?: units.first()
        val active = openPractice()
        val invalid = active != null && (
            active.items.any { it.wordId !in words } ||
                active.session.kind == PracticeKind.LEARN && units.none { it.id == active.session.unitId }
            )
        if (invalid) {
            dao.updateSession(requireNotNull(active).session.copy(openSlot = null, feedbackInput = null))
        } else {
            action(date, unit, active)
        }
        val learned = dao.learnedWords()
        val remaining = remainingNewWords(learned, day)
        val schedules = dao.schedules().filter { it.wordId in positions }
        TaskSnapshot(
            unit, date, newWords(unit, learned, remaining).size, remaining,
            schedules.count { it.effectiveDay <= day }, openPractice(), dao.finishedOn(day),
            schedules.map { it.effectiveDay }.filter { it > day }.minOrNull()?.let(LocalDate::ofEpochDay),
            libraryUpdated = invalid,
        )
    }

    private suspend fun openPractice() = dao.openSession()?.let { Practice(it, dao.items(it.sessionId)) }

    private suspend fun confirmWord(active: Practice, date: LocalDate) {
        val item = active.item
        val day = date.toEpochDay()
        val learned = dao.learnedWords()
        if (item.learnCompletedDay != null || learned.any { it.wordId == item.wordId }) return
        val session = active.session
        if (remainingNewWords(learned, day) == 0) {
            summarize(session.copy(endedByDailyLimit = true), date)
            return
        }
        dao.insertLearned(LearnedWord(item.wordId, day))
        dao.insertSchedules(QuestionType.entries.map { ReviewSchedule(item.wordId, it, date.plusDays(1).toEpochDay()) })
        dao.updateItem(item.copy(learnCompletedDay = day))
        if (item.ordinal == active.items.lastIndex) summarize(session, date)
        else dao.updateSession(session.copy(currentOrdinal = item.ordinal + 1))
    }

    private suspend fun recordReview(active: Practice, date: LocalDate, outcome: Outcome, input: String?) {
        val session = active.session
        val item = active.item
        val updated = if (session.phase == Phase.RETRY) {
            if (item.retryOrdinal == null || item.retryOutcome != null) return
            item.copy(retryOutcome = outcome)
        } else {
            if (item.firstOutcome != null) return
            val old = dao.schedule(item.wordId, active.question.type)
            val schedule = old.afterFirst(outcome, date)
            dao.updateSchedule(schedule)
            item.copy(
                firstOutcome = outcome,
                firstResolvedDay = date.toEpochDay(),
                firstNextDueDay = schedule.dueDay.takeUnless { outcome == Outcome.SKIPPED_AUDIO },
            )
        }
        dao.updateItem(updated)
        if (active.question.type == QuestionType.MEANING || outcome == Outcome.SKIPPED_AUDIO) {
            advanceReview(Practice(session, dao.items(session.sessionId)), date)
        } else {
            dao.updateSession(session.copy(stage = Stage.FEEDBACK, feedbackInput = input))
        }
    }

    private suspend fun advanceReview(active: Practice, date: LocalDate) {
        val session = active.session.copy(stage = Stage.PROMPT, feedbackInput = null)
        if (session.phase == Phase.RETRY) {
            val next = active.items.firstOrNull { it.retryOrdinal == active.index + 1 }
            if (next == null) summarize(session, date)
            else dao.updateSession(session.copy(currentOrdinal = next.ordinal))
        } else if (active.item.ordinal < active.items.lastIndex) {
            dao.updateSession(session.copy(currentOrdinal = active.item.ordinal + 1))
        } else {
            val retry = retryQueue(active.items)
            if (retry.isEmpty()) {
                summarize(session, date)
            } else {
                dao.updateItems(retry.mapIndexed { index, item -> item.copy(retryOrdinal = index) })
                dao.updateSession(session.copy(phase = Phase.RETRY, currentOrdinal = retry.first().ordinal))
            }
        }
    }

    private suspend fun summarize(session: PracticeSession, date: LocalDate) {
        dao.updateSession(session.copy(
            phase = Phase.SUMMARY, stage = Stage.SUMMARY, currentOrdinal = null,
            feedbackInput = null, finishedDay = date.toEpochDay(),
        ))
    }
}
