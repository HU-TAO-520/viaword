package com.viaword.demo

import com.viaword.demo.ui.ActivePractice
import com.viaword.demo.ui.PracticeType
import com.viaword.demo.ui.TodayState
import com.viaword.demo.ui.todayState
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayStateTest {
    @Test
    fun showsNewWordsOnlyWhenReviewIsClear() {
        assertEquals(TodayState.NewWords, todayState(6, 0, null, false))
        assertEquals(TodayState.DueReview, todayState(6, 24, null, false))
    }

    @Test
    fun prioritizesEitherActivePracticeOverOtherTasks() {
        val review = ActivePractice(PracticeType.Review, 4, 12)
        val study = ActivePractice(PracticeType.Study, 2, 5)
        assertEquals(TodayState.Reviewing, todayState(6, 12, review, false))
        assertEquals(TodayState.Studying, todayState(3, 12, study, false))
    }

    @Test
    fun distinguishesCompletedAndIdleDaysWhenThereAreNoTasks() {
        assertEquals(TodayState.Finished, todayState(0, 0, null, true))
        assertEquals(TodayState.Idle, todayState(0, 0, null, false))
    }
}
