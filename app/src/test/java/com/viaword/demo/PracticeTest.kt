package com.viaword.demo

import com.viaword.demo.data.sampleUnits
import com.viaword.demo.practice.AnswerInput
import com.viaword.demo.practice.Move
import com.viaword.demo.practice.Outcome
import com.viaword.demo.practice.Question
import com.viaword.demo.practice.QuestionType
import com.viaword.demo.practice.Stage
import com.viaword.demo.practice.grade
import com.viaword.demo.practice.normalizeAnswer
import com.viaword.demo.practice.resolveAnswer
import org.junit.Assert.*
import org.junit.Test

class PracticeTest {
    private val words = sampleUnits.flatMap { it.entries }.associateBy { it.id }

    @Test fun normalizationRetainsInternalPunctuationAndWordBoundaries() {
        assertEquals("take care of", normalizeAnswer("  TAKE   CARE OF  "))
        assertEquals("take care of", normalizeAnswer("\tTAKE\u2003CARE\u00a0OF\t"))
        for (apostrophe in "‘’ʼ＇") assertEquals("don't", normalizeAnswer("don${apostrophe}t"))
        assertNotEquals("take care of", normalizeAnswer("takecareof"))
        assertNotEquals("don't", normalizeAnswer("dont"))
        assertNotEquals("take care of", normalizeAnswer("take-care-of"))
    }

    @Test fun alternativesAreScopedToWordAndQuestionType() {
        for ((id, input) in listOf("sample-1-02" to "GET READY", "sample-2-02" to "helpful")) {
            val question = Question(id, QuestionType.SPELLING)
            val word = words.getValue(id)
            val spelling = grade(question, word, input)
            assertEquals(Outcome.CORRECT, spelling.outcome)
            assertTrue(spelling.usedAlternative)
            assertEquals(Outcome.INCORRECT, grade(question.copy(type = QuestionType.DICTATION), word, input).outcome)
        }
    }

    @Test fun spellingRejectsBlankAndDictationRequiresPlaybackOrError() {
        val word = words.getValue("sample-2-03")
        val spelling = Question(word.id, QuestionType.SPELLING)
        val dictation = spelling.copy(type = QuestionType.DICTATION)
        fun resolve(question: Question, move: Move, input: AnswerInput) =
            resolveAnswer(Stage.PROMPT, question, word, move, input)
        assertNull(resolve(spelling, Move.SUBMIT, AnswerInput(draft = "\t ")))
        assertEquals(Outcome.UNKNOWN, resolve(spelling, Move.UNKNOWN, AnswerInput(draft = "draft")))
        val unheard = AnswerInput(draft = "  TAKE   CARE OF  ")
        assertNull(resolve(dictation, Move.SUBMIT, unheard))
        assertNull(resolve(dictation, Move.UNKNOWN, unheard))
        assertNull(resolve(dictation, Move.SKIP_AUDIO, unheard))
        assertNull(resolve(spelling, Move.SKIP_AUDIO, unheard.copy(audioFailed = true)))
        assertEquals(Outcome.SKIPPED_AUDIO, resolve(dictation, Move.SKIP_AUDIO, unheard.copy(audioFailed = true)))
        assertEquals(Outcome.CORRECT, resolve(dictation, Move.SUBMIT, unheard.copy(heardAudio = true)))
    }
}
