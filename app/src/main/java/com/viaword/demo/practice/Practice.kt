package com.viaword.demo.practice

import com.viaword.demo.data.AppCatalog
import com.viaword.demo.data.PracticeItem
import com.viaword.demo.data.PracticeSession
import com.viaword.demo.data.WordEntry
import java.util.Locale

enum class QuestionType { MEANING, SPELLING, DICTATION }
enum class Outcome { CORRECT, INCORRECT, UNKNOWN, SKIPPED_AUDIO }
internal enum class PracticeKind { LEARN, REVIEW }
internal enum class Phase { LEARN, FIRST, RETRY, SUMMARY }
internal enum class Stage { CARD, PROMPT, REVEALED, FEEDBACK, SUMMARY }

data class Question(val wordId: String, val type: QuestionType)
internal data class PracticeStep(val sessionId: Long, val ordinal: Int?, val phase: Phase, val stage: Stage)

internal data class Practice(
    val session: PracticeSession,
    val items: List<PracticeItem>,
    val input: AnswerInput = AnswerInput(),
) {
    val step get() = PracticeStep(session.sessionId, session.currentOrdinal, session.phase, session.stage)
    val item get() = items[requireNotNull(session.currentOrdinal)]
    val question get() = Question(item.wordId, requireNotNull(item.questionType))
    val total get() = if (session.phase == Phase.RETRY) items.count { it.retryOrdinal != null } else items.size
    val index get() = if (session.phase == Phase.RETRY) requireNotNull(item.retryOrdinal) else item.ordinal
    val completed get() = items.count { it.learnCompletedDay != null || it.firstOutcome != null }
    val canAnswer get() = input.canAnswer(question.type)
    val canSubmit get() = canAnswer && normalizeAnswer(input.draft).isNotEmpty()
    val feedbackOutcome get() = requireNotNull(
        if (session.phase == Phase.RETRY) item.retryOutcome else item.firstOutcome,
    )
}

internal data class AnswerInput(
    val draft: String = "",
    val heardAudio: Boolean = false,
    val audioFailed: Boolean = false,
) {
    fun canAnswer(type: QuestionType) = type != QuestionType.DICTATION || heardAudio
}

private val alternatives = mapOf(
    Question("sample-1-02", QuestionType.SPELLING) to listOf("get ready", "make preparations"),
    Question("sample-1-05", QuestionType.SPELLING) to listOf("make better"),
    Question("sample-2-02", QuestionType.SPELLING) to listOf("helpful"),
)

fun normalizeAnswer(input: String): String = buildString {
    var spacePending = false
    for (char in input.lowercase(Locale.ROOT)) {
        if (char.isWhitespace()) {
            spacePending = isNotEmpty()
        } else {
            if (spacePending) append(' ')
            spacePending = false
            append(if (char in "‘’ʼ＇") '\'' else char)
        }
    }
}

internal data class Grade(val outcome: Outcome, val usedAlternative: Boolean = false)

internal fun grade(question: Question, word: WordEntry, input: String): Grade {
    val normalized = normalizeAnswer(input)
    val standard = normalizeAnswer(word.english)
    val accepted = AppCatalog.aliases[question] ?: alternatives[question].orEmpty()
    val usedAlternative = normalized != standard && accepted.any { normalizeAnswer(it) == normalized }
    return Grade(
        if (normalized == standard || usedAlternative) Outcome.CORRECT else Outcome.INCORRECT,
        usedAlternative,
    )
}

internal sealed interface PracticeAction {
    data class Draft(val input: String) : PracticeAction
    data class AudioResult(val played: Boolean) : PracticeAction
}

internal enum class Move : PracticeAction {
    NEXT_WORD, REVEAL, REMEMBERED, FORGOT, SUBMIT, UNKNOWN, SKIP_AUDIO, CONTINUE, CLOSE
}

internal fun resolveAnswer(
    stage: Stage,
    question: Question,
    word: WordEntry,
    move: Move,
    input: AnswerInput,
): Outcome? = when (question.type) {
    QuestionType.MEANING -> if (stage == Stage.REVEALED) {
        when (move) {
            Move.REMEMBERED -> Outcome.CORRECT
            Move.FORGOT -> Outcome.UNKNOWN
            else -> null
        }
    } else null
    else -> if (stage == Stage.PROMPT) {
        when {
            move == Move.SKIP_AUDIO && question.type == QuestionType.DICTATION && input.audioFailed ->
                Outcome.SKIPPED_AUDIO
            !input.canAnswer(question.type) -> null
            move == Move.UNKNOWN -> Outcome.UNKNOWN
            move == Move.SUBMIT && normalizeAnswer(input.draft).isNotEmpty() ->
                grade(question, word, input.draft).outcome
            else -> null
        }
    } else null
}
