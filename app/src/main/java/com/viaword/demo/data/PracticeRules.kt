package com.viaword.demo.data

import com.viaword.demo.practice.Outcome
import java.time.LocalDate

internal const val DAILY_WORD_LIMIT = 6
internal const val REVIEW_ROUND_LIMIT = 18

internal fun remainingNewWords(learned: List<LearnedWord>, day: Long) =
    (DAILY_WORD_LIMIT - learned.count { it.learnedDay == day }).coerceAtLeast(0)

internal fun newWords(unit: WordUnit, learned: List<LearnedWord>, remaining: Int): List<WordEntry> {
    val known = learned.mapTo(hashSetOf()) { it.wordId }
    return unit.entries.filterNot { it.id in known }.take(remaining)
}

internal val ReviewSchedule.effectiveDay get() = maxOf(dueDay, audioRetryDay ?: dueDay)

internal fun ReviewSchedule.afterFirst(outcome: Outcome, day: LocalDate): ReviewSchedule = when (outcome) {
    Outcome.CORRECT -> {
        val streak = (correctStreak + 1).coerceAtMost(4)
        val days = listOf(3L, 7L, 14L, 30L)
        copy(correctStreak = streak, dueDay = day.plusDays(days[streak - 1]).toEpochDay(), audioRetryDay = null)
    }
    Outcome.INCORRECT, Outcome.UNKNOWN ->
        copy(correctStreak = 0, dueDay = day.plusDays(1).toEpochDay(), audioRetryDay = null)
    Outcome.SKIPPED_AUDIO -> copy(audioRetryDay = day.plusDays(1).toEpochDay())
}

internal fun dueReviews(schedules: List<ReviewSchedule>, positions: Map<String, Int>, day: Long) =
    schedules.filter { it.wordId in positions && it.effectiveDay <= day }
        .sortedWith(compareBy({ it.dueDay }, { positions.getValue(it.wordId) }, { it.questionType.ordinal }))

internal fun reviewQueue(due: List<ReviewSchedule>): List<ReviewSchedule> = buildList {
    for (group in due.take(REVIEW_ROUND_LIMIT).groupBy { it.dueDay }.values) {
        addAll(spreadWords(group, lastOrNull()?.wordId, ReviewSchedule::wordId))
    }
}

internal fun <T> spreadWords(items: List<T>, previousWord: String?, wordId: (T) -> String): List<T> = buildList {
    val remaining = items.toMutableList()
    var previous = previousWord
    while (remaining.isNotEmpty()) {
        val counts = remaining.groupingBy(wordId).eachCount()
        val choice = remaining.indices.filter { wordId(remaining[it]) != previous }
            .maxByOrNull { counts.getValue(wordId(remaining[it])) } ?: 0
        val next = remaining.removeAt(choice)
        add(next)
        previous = wordId(next)
    }
}

internal val Outcome?.needsRetry get() = this == Outcome.INCORRECT || this == Outcome.UNKNOWN

internal fun retryQueue(items: List<PracticeItem>) =
    spreadWords(items.filter { it.firstOutcome.needsRetry }, items.last().wordId, PracticeItem::wordId)

internal data class ReviewSummary(
    val correct: Int,
    val missed: Int,
    val skipped: Int,
    val dueTomorrow: Boolean,
)

internal fun reviewSummary(items: List<PracticeItem>, today: LocalDate): ReviewSummary {
    val missed = items.filter { it.firstOutcome.needsRetry }
    return ReviewSummary(
        items.count { it.firstOutcome == Outcome.CORRECT },
        missed.size,
        items.count { it.firstOutcome == Outcome.SKIPPED_AUDIO },
        missed.all { it.firstNextDueDay == today.plusDays(1).toEpochDay() },
    )
}
