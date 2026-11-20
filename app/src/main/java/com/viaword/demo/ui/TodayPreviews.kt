package com.viaword.demo.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.viaword.demo.data.sampleUnits
import java.time.LocalDate

@Preview(name = "01 · New words", widthDp = 390, heightDp = 740, locale = "zh-rCN")
@Composable
private fun NewWordsPreview() = TodayPreview(newWords = 6)

@Preview(name = "02 · Due review", widthDp = 390, heightDp = 740, locale = "zh-rCN")
@Composable
private fun DueReviewPreview() = TodayPreview(newWords = 6, due = 24)

@Preview(name = "03 · Continue review", widthDp = 390, heightDp = 740, locale = "zh-rCN")
@Composable
private fun ContinueReviewPreview() = TodayPreview(
    newWords = 6, due = 12, active = ActivePractice(PracticeType.Review, 4, 12),
)

@Preview(name = "04 · Continue study", widthDp = 390, heightDp = 740, locale = "zh-rCN")
@Composable
private fun ContinueStudyPreview() = TodayPreview(
    newWords = 3, due = 12, active = ActivePractice(PracticeType.Study, 2, 5),
)

@Preview(name = "05 · Finished", widthDp = 390, heightDp = 740, locale = "zh-rCN")
@Composable
private fun FinishedPreview() = TodayPreview(finished = true, nextReview = LocalDate.of(2026, 11, 15))

@Preview(name = "06 · Idle", widthDp = 390, heightDp = 740, locale = "zh-rCN")
@Composable
private fun IdlePreview() = TodayPreview()

@Composable
private fun TodayPreview(
    newWords: Int = 0,
    due: Int = 0,
    active: ActivePractice? = null,
    finished: Boolean = false,
    nextReview: LocalDate? = null,
) {
    ViawordTheme {
        RootLayout(RootTab.Today, onTab = {}) {
            TodayScreen(
                sampleUnits.first(), newWords, due, active, finished, nextReview, onOpenUnit = {}, onOpenSettings = {},
            )
        }
    }
}
