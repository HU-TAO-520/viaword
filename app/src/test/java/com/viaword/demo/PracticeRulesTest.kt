package com.viaword.demo

import com.viaword.demo.data.*
import com.viaword.demo.practice.Outcome
import com.viaword.demo.practice.QuestionType
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PracticeRulesTest {
    private val firstDay = LocalDate.of(2026, 11, 14)
    private val day = firstDay.toEpochDay()
    private val ids = sampleUnits.flatMap { unit -> unit.entries.map { it.id } }
    private val positions = ids.withIndex().associate { (index, id) -> id to index }

    @Test fun u1QuotaIsGlobalAndCountsOnlyConfirmationDates() {
        val learned = ids.takeLast(3).map { LearnedWord(it, day) }
        fun available(records: List<LearnedWord>, date: Long) =
            newWords(sampleUnits.first(), records, remainingNewWords(records, date)).map { it.id }
        val planned = available(learned, day)
        assertEquals(ids.take(3), planned)
        val completed = learned + planned.take(2).map { LearnedWord(it, day) } + LearnedWord(planned.last(), day + 1)
        assertEquals(1, remainingNewWords(completed, day))
        assertEquals(5, remainingNewWords(completed, day + 1))
        assertEquals(ids.subList(3, 6), available(completed, day + 1))
        val atLimit = completed + LearnedWord(ids[3], day)
        assertEquals(0, remainingNewWords(atLimit, day))
        assertTrue(available(atLimit, day).isEmpty())
    }

    @Test fun u2CalendarIntervalsUseFirstAnswerDayAndKeepQuestionTypesIndependent() {
        val (meaning, spelling, dictation) = QuestionType.entries.map {
            ReviewSchedule(ids.first(), it, firstDay.plusDays(1).toEpochDay())
        }
        val nov15 = firstDay.plusDays(1)
        val nov18 = nov15.plusDays(3)
        assertTrue(listOf(meaning, spelling, dictation).all { it.dueDay == nov15.toEpochDay() && it.correctStreak == 0 })
        var recall = meaning.afterFirst(Outcome.CORRECT, nov15)
        var written = spelling.afterFirst(Outcome.CORRECT, nov15)
        var heard = dictation.afterFirst(Outcome.INCORRECT, nov15)
        assertEquals(nov18.toEpochDay(), recall.dueDay)
        assertEquals(nov18.toEpochDay(), written.dueDay)
        assertEquals(nov15.plusDays(1).toEpochDay(), heard.dueDay)
        heard = heard.afterFirst(Outcome.CORRECT, nov15.plusDays(1))
        assertEquals(nov18.plusDays(1).toEpochDay(), heard.dueDay)
        written = written.afterFirst(Outcome.UNKNOWN, nov18)
        assertEquals(0, written.correctStreak)
        assertEquals(nov18.plusDays(1).toEpochDay(), written.dueDay)
        written = written.afterFirst(Outcome.INCORRECT, nov18.plusDays(5))
        assertEquals(nov18.plusDays(6).toEpochDay(), written.dueDay)
        assertEquals(0, written.correctStreak)
        assertEquals(1, written.afterFirst(Outcome.CORRECT, nov18.plusDays(6)).correctStreak)
        listOf("2026-11-25", "2026-12-09", "2027-01-08", "2027-02-07").forEachIndexed { index, next ->
            recall = recall.afterFirst(Outcome.CORRECT, LocalDate.ofEpochDay(recall.dueDay))
            assertEquals(LocalDate.parse(next).toEpochDay(), recall.dueDay)
            assertEquals((index + 2).coerceAtMost(4), recall.correctStreak)
        }
        assertEquals(nov18.plusDays(1).toEpochDay(), heard.dueDay)
    }

    @Test fun u3ReviewLimitsSelectionBeforeReorderingAndUsesStableTieBreaks() {
        val schedules = ids.flatMapIndexed { index, id ->
            QuestionType.entries.map { ReviewSchedule(id, it, if (index < 6) day else day + 1) }
        }
        val due = dueReviews(schedules.reversed(), positions, day + 2)
        assertEquals(27, due.size)
        val queue = reviewQueue(due)
        assertEquals(schedules.take(18).toSet(), queue.toSet())
        assertEquals(queue, reviewQueue(dueReviews(schedules, positions, day + 2)))
        assertTrue(queue.zipWithNext().all { (a, b) -> a.wordId != b.wordId })
        val next = dueReviews(schedules.map {
            if (it in queue) it.afterFirst(Outcome.CORRECT, firstDay.plusDays(2)) else it
        }, positions, day + 2)
        assertEquals(schedules.drop(18).toSet(), reviewQueue(next).toSet())
        val uneven = schedules.take(2) + schedules.subList(3, 6)
        val (a, b) = ids
        assertEquals(listOf(b, a, b, a, b), reviewQueue(uneven).map { it.wordId })
        val twoDates = schedules.take(1) + schedules.takeLast(3) + schedules.subList(1, 3)
        assertEquals(twoDates.map { it.dueDay }.sorted(), reviewQueue(dueReviews(twoDates, positions, day + 1)).map { it.dueDay })
    }

    @Test fun u4RetryOrderAndSummaryUseFirstResultsAndSavedNextDays() {
        val items = listOf(Outcome.CORRECT, Outcome.INCORRECT, Outcome.UNKNOWN, Outcome.SKIPPED_AUDIO)
            .mapIndexed { index, outcome ->
                PracticeItem(
                    1, index, ids[index], QuestionType.entries[index % 3], firstOutcome = outcome,
                    firstResolvedDay = day, firstNextDueDay = if (outcome.needsRetry) day + 1 else null,
                )
            }
        assertEquals(listOf(1, 2), retryQueue(items).map { it.ordinal })
        val retried = items.map {
            when (it.ordinal) {
                1 -> it.copy(retryOrdinal = 0, retryOutcome = Outcome.CORRECT)
                2 -> it.copy(retryOrdinal = 1, retryOutcome = Outcome.INCORRECT)
                else -> it
            }
        }
        assertEquals(ReviewSummary(1, 2, 1, true), reviewSummary(retried, firstDay))
        assertEquals(ReviewSummary(1, 2, 1, false), reviewSummary(retried, firstDay.plusDays(1)))
    }

    @Test fun u5AudioSkipPreservesOriginalDueAndStreakAndWaitsUntilTomorrow() {
        val schedule = ReviewSchedule(ids.first(), QuestionType.DICTATION, day - 2, correctStreak = 2)
        val skipped = schedule.afterFirst(Outcome.SKIPPED_AUDIO, firstDay)
        assertEquals(schedule.copy(audioRetryDay = day + 1), skipped)
        assertEquals(day + 1, skipped.effectiveDay)
        assertTrue(dueReviews(listOf(skipped), positions, day).isEmpty())
        assertEquals(listOf(skipped), reviewQueue(dueReviews(listOf(skipped), positions, day + 1)))
        assertNull(skipped.afterFirst(Outcome.CORRECT, firstDay.plusDays(1)).audioRetryDay)
    }
}
