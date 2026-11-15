package com.viaword.demo

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.viaword.demo.data.*
import com.viaword.demo.practice.*
import com.viaword.demo.ui.ActivePractice
import com.viaword.demo.ui.PracticeType
import com.viaword.demo.ui.TodayState
import com.viaword.demo.ui.todayState
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PracticeStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val firstDay = LocalDate.of(2026, 11, 14)
    private var date = firstDay
    private var dateReads = 0
    private val units = sampleUnits
    private var unit = units.first()
    private lateinit var db: PracticeDatabase
    private lateinit var store: PracticeStore
    private val dbName = "test-${UUID.randomUUID()}.db"
    private val dao get() = db.practiceDao()

    @After fun cleanUp() {
        if (::db.isInitialized) db.close()
        context.deleteDatabase(dbName)
    }

    private fun connect(file: Boolean = false, catalog: List<WordUnit> = units) {
        if (::db.isInitialized) db.close()
        db = if (file) Room.databaseBuilder(context, PracticeDatabase::class.java, dbName).build()
        else Room.inMemoryDatabaseBuilder(context, PracticeDatabase::class.java).build()
        store = PracticeStore(db, { unit.id }, { dateReads++; date }, catalog)
    }

    private val TaskSnapshot.round get() = requireNotNull(practice)

    private suspend fun move(move: Move, input: AnswerInput = AnswerInput()) =
        store.move(store.read().round.step, move, input)

    private suspend fun learnUnit() {
        val batch = store.start(PracticeKind.LEARN, unit.id).round
        repeat(batch.total) { move(Move.NEXT_WORD) }
        move(Move.CLOSE)
    }

    private suspend fun tomorrowReview(): Practice {
        learnUnit()
        date = firstDay.plusDays(1)
        return store.start(PracticeKind.REVIEW, unit.id).round
    }

    private fun correctInput(round: Practice) = AnswerInput(
        draft = units.flatMap { it.entries }.first { it.id == round.item.wordId }.english,
        heardAudio = true,
    )

    @Test fun u1QuotaRecheckedOnEveryConfirmationIncludingClockRollback() = runBlocking {
        connect()
        unit = units.last()
        learnUnit()
        unit = units.first()
        val batch = store.start(PracticeKind.LEARN, unit.id).round
        assertEquals(unit.entries.take(3).map { it.id }, batch.items.map { it.wordId })
        assertEquals(3, dao.learnedWords().size)
        repeat(3) { move(Move.NEXT_WORD) }
        move(Move.CLOSE)
        assertEquals(0, store.read().availableNewCount)
        assertNull(store.start(PracticeKind.LEARN, unit.id).practice)

        date = firstDay.plusDays(1)
        val next = store.start(PracticeKind.LEARN, unit.id).round
        assertEquals(unit.entries.drop(3).map { it.id }, next.items.map { it.wordId })
        move(Move.NEXT_WORD)
        date = firstDay
        val stopped = move(Move.NEXT_WORD).round
        assertTrue(stopped.session.endedByDailyLimit)
        assertEquals(1, stopped.completed)
        assertEquals(7, dao.learnedWords().size)
        assertEquals(21, dao.schedules().size)
        move(Move.CLOSE)

        date = firstDay.plusDays(1)
        assertEquals(2, store.read().availableNewCount)
        store.start(PracticeKind.LEARN, unit.id)
        move(Move.NEXT_WORD)
        date = firstDay.plusDays(2)
        val before = dateReads
        val finished = store.move(store.read().round.step, Move.NEXT_WORD)
        assertEquals(before + 2, dateReads)
        assertEquals(5, finished.remainingNewWords)
        assertEquals(2, finished.round.completed)
        assertEquals(date.toEpochDay(), dao.learnedWords().single { it.wordId == unit.entries.last().id }.learnedDay)
        assertEquals(date.plusDays(1).toEpochDay(), dao.schedule(unit.entries.last().id, QuestionType.MEANING).dueDay)
    }

    @Test fun r1DuplicateAndConcurrentEventsCannotRescoreOrAdvanceAnotherStep() = runBlocking {
        connect()
        suspend fun duplicate(action: suspend () -> TaskSnapshot) = coroutineScope {
            List(3) { async { action() } }.awaitAll()
        }
        val started = duplicate { store.start(PracticeKind.LEARN, unit.id) }
        assertEquals(1, started.map { it.round.session.sessionId }.distinct().size)
        val card = started.first().round.step
        duplicate { store.move(card, Move.NEXT_WORD) }
        assertEquals(1, dao.learnedWords().size)
        assertEquals(QuestionType.entries.toSet(), dao.schedules().map { it.questionType }.toSet())
        assertEquals(1, store.read().round.item.ordinal)
        repeat(5) { move(Move.NEXT_WORD) }
        move(Move.CLOSE)
        date = firstDay.plusDays(1)
        val reviews = duplicate { store.start(PracticeKind.REVIEW, unit.id) }
        val prompt = reviews.first().round.step
        assertEquals(1, reviews.map { it.round.session.sessionId }.distinct().size)
        assertNotEquals(card.sessionId, prompt.sessionId)
        assertEquals(prompt, store.move(card, Move.NEXT_WORD).round.step)
        val revealed = move(Move.REVEAL).round.step
        duplicate { store.move(revealed, Move.REMEMBERED) }
        assertEquals(1, store.read().round.completed)
        while (store.read().round.question.type == QuestionType.MEANING) {
            move(Move.REVEAL)
            move(Move.REMEMBERED)
        }
        val question = store.read().round
        val feedback = duplicate { store.move(question.step, Move.SUBMIT, correctInput(question)) }.first().round
        assertEquals(Stage.FEEDBACK, feedback.session.stage)
        assertEquals(Outcome.CORRECT, feedback.item.firstOutcome)
        val expected = dao.schedule(question.item.wordId, question.question.type)
        assertEquals(1, expected.correctStreak)
        assertEquals(date.plusDays(3).toEpochDay(), expected.dueDay)
        val advanced = duplicate { store.move(feedback.step, Move.CONTINUE) }.first().round
        assertEquals(question.item.ordinal + 1, advanced.item.ordinal)
        assertEquals(advanced.step, store.move(question.step, Move.SUBMIT, correctInput(question)).round.step)
        assertEquals(advanced.step, store.move(revealed, Move.FORGOT).round.step)
        assertEquals(expected, dao.schedule(question.item.wordId, question.question.type))
    }

    @Test fun r2FailureAfterLearnedInsertOrScheduleUpdateRollsBackTheWholeEvent() = runBlocking {
        connect()
        val card = store.start(PracticeKind.LEARN, unit.id).round.step
        val sql = db.openHelper.writableDatabase
        sql.execSQL("CREATE TRIGGER reject_schedule BEFORE INSERT ON review_schedule BEGIN SELECT RAISE(ABORT, 'test'); END")
        expectConstraint { store.move(card, Move.NEXT_WORD) }
        assertEquals(card, store.read().round.step)
        assertTrue(dao.learnedWords().isEmpty())
        assertTrue(dao.schedules().isEmpty())
        sql.execSQL("DROP TRIGGER reject_schedule")
        val nextCard = store.move(card, Move.NEXT_WORD).round
        assertEquals(1, nextCard.completed)
        assertEquals(3, dao.schedules().size)
        repeat(5) { move(Move.NEXT_WORD) }
        move(Move.CLOSE)
        date = firstDay.plusDays(1)
        store.start(PracticeKind.REVIEW, unit.id)
        val revealed = move(Move.REVEAL).round
        val original = dao.schedule(revealed.item.wordId, revealed.question.type)
        sql.execSQL("CREATE TRIGGER reject_result BEFORE UPDATE ON practice_item BEGIN SELECT RAISE(ABORT, 'test'); END")
        expectConstraint { store.move(revealed.step, Move.REMEMBERED) }
        assertEquals(revealed, store.read().round)
        assertEquals(original, dao.schedule(original.wordId, original.questionType))
        sql.execSQL("DROP TRIGGER reject_result")
        assertEquals(1, store.move(revealed.step, Move.REMEMBERED).round.completed)
        assertEquals(1, store.move(revealed.step, Move.REMEMBERED).round.completed)
        assertEquals(1, dao.schedule(original.wordId, original.questionType).correctStreak)
    }

    private suspend fun expectConstraint(write: suspend () -> TaskSnapshot) {
        try {
            write()
            fail("Expected test trigger to abort the transaction")
        } catch (_: SQLiteConstraintException) {
            // The caller checks the entire state after rollback.
        }
    }

    @Test fun r3FileReopenRestoresRevealedFeedbackRetryAndSummary() = runBlocking {
        connect(file = true)
        tomorrowReview()
        val revealed = move(Move.REVEAL).round
        connect(file = true)
        assertEquals(revealed, store.read().round)
        move(Move.FORGOT)
        while (store.read().round.question.type != QuestionType.SPELLING) answerCorrectly()
        val spelling = store.read().round
        val feedback = move(Move.SUBMIT, AnswerInput(draft = " wrong answer ")).round
        val schedules = dao.schedules()
        connect(file = true)
        assertEquals(feedback, store.read().round)
        assertEquals(" wrong answer ", store.read().round.session.feedbackInput)
        assertEquals(schedules, dao.schedules())
        assertEquals(feedback, store.move(spelling.step, Move.SUBMIT, correctInput(spelling)).round)
        move(Move.CONTINUE)
        while (store.read().round.session.phase == Phase.FIRST) answerCorrectly()
        val retry = store.read().round
        assertEquals(Phase.RETRY, retry.session.phase)
        assertEquals(2, retry.total)
        val firstResults = retry.items.map { it.firstOutcome }
        val afterFirst = dao.schedules()
        connect(file = true)
        assertEquals(retry, store.read().round)
        answerCorrectly()
        val second = store.read().round
        connect(file = true)
        assertEquals(second, store.read().round)
        answerCorrectly()
        val summary = store.read().round
        assertEquals(Phase.SUMMARY, summary.session.phase)
        assertEquals(firstResults, summary.items.map { it.firstOutcome })
        assertEquals(afterFirst, dao.schedules())
        connect(file = true)
        assertEquals(summary, store.read().round)
        move(Move.CLOSE)
        assertNull(store.read().practice)
        assertTrue(store.read().finishedTaskToday)
    }

    private suspend fun answerCorrectly() {
        val round = store.read().round
        if (round.question.type == QuestionType.MEANING) {
            move(Move.REVEAL)
            move(Move.REMEMBERED)
        } else {
            move(Move.SUBMIT, correctInput(round))
            move(Move.CONTINUE)
        }
    }

    @Test fun u5FirstAndRetryAudioSkipsHaveDifferentSchedulingEffects() = runBlocking {
        connect()
        tomorrowReview()
        var skippedId: String? = null
        var missedId: String? = null
        while (store.read().round.session.phase == Phase.FIRST) {
            val active = store.read().round
            if (active.question.type != QuestionType.DICTATION || missedId != null) {
                answerCorrectly()
            } else if (skippedId == null) {
                skippedId = active.item.wordId
                val before = dao.schedule(skippedId, QuestionType.DICTATION)
                move(Move.SKIP_AUDIO, AnswerInput(audioFailed = true))
                assertEquals(before.copy(audioRetryDay = date.plusDays(1).toEpochDay()),
                    dao.schedule(skippedId, QuestionType.DICTATION))
            } else {
                missedId = active.item.wordId
                move(Move.SUBMIT, AnswerInput(draft = "wrong", heardAudio = true))
                move(Move.CONTINUE)
            }
        }
        val retry = store.read().round
        assertEquals(1, retry.total)
        assertEquals(missedId, retry.item.wordId)
        val schedules = dao.schedules()
        val finished = move(Move.SKIP_AUDIO, AnswerInput(audioFailed = true)).round
        assertEquals(schedules, dao.schedules())
        assertEquals(Outcome.INCORRECT, finished.items[retry.item.ordinal].firstOutcome)
        assertEquals(Outcome.SKIPPED_AUDIO, finished.items[retry.item.ordinal].retryOutcome)
        move(Move.CLOSE)
        assertEquals(0, store.read().dueQuestionCount)
        assertNull(store.start(PracticeKind.REVIEW, unit.id).practice)
        date = date.plusDays(1)
        assertEquals(2, store.read().dueQuestionCount)
        assertEquals(setOf(skippedId, missedId), store.start(PracticeKind.REVIEW, unit.id).round.items.map { it.wordId }.toSet())
    }

    @Test fun u6HomeSnapshotPrioritizesOpenRoundsAndUsesEffectiveFutureDates() = runBlocking {
        connect()
        fun state(s: TaskSnapshot) = todayState(
            s.availableNewCount, s.dueQuestionCount, s.practice?.let {
                val type = if (it.session.kind == PracticeKind.LEARN) PracticeType.Study else PracticeType.Review
                ActivePractice(type, it.completed, it.items.size)
            }, s.finishedTaskToday,
        )
        assertEquals(TodayState.NewWords, state(store.read()))
        assertNull(store.start(PracticeKind.REVIEW, unit.id).practice)
        unit = units.last()
        assertEquals(TodayState.Studying, state(store.start(PracticeKind.LEARN, unit.id)))
        repeat(3) { move(Move.NEXT_WORD) }
        assertEquals(Phase.SUMMARY, store.read().round.session.phase)
        assertEquals(TodayState.Studying, state(store.read()))
        val done = move(Move.CLOSE)
        assertEquals(TodayState.Finished, state(done))
        assertEquals(firstDay.plusDays(1), done.nextReviewDate)
        date = firstDay.plusDays(1)
        assertEquals(9, store.read().dueQuestionCount)
        assertEquals(TodayState.DueReview, state(store.read()))
        unit = units.first()
        assertEquals(6, store.read().availableNewCount)
        assertEquals(TodayState.DueReview, state(store.read()))
        store.start(PracticeKind.REVIEW, unit.id)
        move(Move.REVEAL)
        move(Move.FORGOT)
        while (store.read().round.session.phase == Phase.FIRST) answerCorrectly()
        assertEquals(Phase.RETRY, store.read().round.session.phase)
        assertEquals(TodayState.Reviewing, state(store.read()))
        answerCorrectly()
        assertEquals(TodayState.Reviewing, state(store.read()))
        move(Move.CLOSE)
        unit = units.last()
        val postponed = date.plusDays(2).toEpochDay()
        for (schedule in dao.schedules()) dao.updateSchedule(schedule.copy(audioRetryDay = postponed))
        date = date.plusDays(1)
        assertEquals(TodayState.Idle, state(store.read()))
        assertEquals(LocalDate.ofEpochDay(postponed), store.read().nextReviewDate)
        assertNull(store.start(PracticeKind.LEARN, unit.id).practice)
    }

    @Test fun removedWordClosesOpenRoundButKeepsHistoryAndGlobalQuota() = runBlocking {
        connect(file = true)
        val round = store.start(PracticeKind.LEARN, unit.id).round
        move(Move.NEXT_WORD)
        val shortened = unit.copy(entries = unit.entries.drop(1))
        connect(file = true, catalog = listOf(shortened, units.last()))
        val refreshed = store.read()
        assertTrue(refreshed.libraryUpdated)
        assertNull(refreshed.practice)
        assertEquals(5, refreshed.remainingNewWords)
        assertEquals(1, dao.learnedWords().size)
        assertEquals(3, dao.schedules().size)
        assertEquals(firstDay.toEpochDay(), dao.items(round.session.sessionId).first().learnCompletedDay)
        date = firstDay.plusDays(1)
        assertEquals(0, store.read().dueQuestionCount)
        assertNull(store.read().nextReviewDate)
        assertNull(store.start(PracticeKind.REVIEW, unit.id).practice)
    }
}
