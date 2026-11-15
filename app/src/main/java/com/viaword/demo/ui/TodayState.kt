package com.viaword.demo.ui

enum class PracticeType { Study, Review }

data class ActivePractice(
    val type: PracticeType,
    val completed: Int,
    val total: Int,
    val unitTitle: String? = null,
    val summary: Boolean = false,
    val retry: Boolean = false,
)

enum class TodayState { Studying, Reviewing, DueReview, NewWords, Finished, Idle }

fun todayState(
    availableNewCount: Int,
    dueQuestionCount: Int,
    activePractice: ActivePractice?,
    finishedTaskToday: Boolean,
): TodayState = when {
    activePractice?.type == PracticeType.Study -> TodayState.Studying
    activePractice?.type == PracticeType.Review -> TodayState.Reviewing
    dueQuestionCount > 0 -> TodayState.DueReview
    availableNewCount > 0 -> TodayState.NewWords
    finishedTaskToday -> TodayState.Finished
    else -> TodayState.Idle
}
