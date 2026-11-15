package com.viaword.demo.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.viaword.demo.practice.Outcome
import com.viaword.demo.practice.Phase
import com.viaword.demo.practice.PracticeKind
import com.viaword.demo.practice.QuestionType
import com.viaword.demo.practice.Stage

@Entity(tableName = "learned_word", indices = [Index("learnedDay")])
internal data class LearnedWord(@PrimaryKey val wordId: String, val learnedDay: Long)

@Entity(tableName = "review_schedule", primaryKeys = ["wordId", "questionType"])
internal data class ReviewSchedule(
    val wordId: String,
    val questionType: QuestionType,
    val dueDay: Long,
    val correctStreak: Int = 0,
    val audioRetryDay: Long? = null,
)

@Entity(tableName = "practice_session", indices = [Index("openSlot", unique = true)])
internal data class PracticeSession(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val openSlot: Int? = 1,
    val kind: PracticeKind,
    val unitId: String? = null,
    val phase: Phase,
    val stage: Stage,
    val currentOrdinal: Int? = 0,
    val feedbackInput: String? = null,
    val finishedDay: Long? = null,
    @ColumnInfo(defaultValue = "0") val endedByDailyLimit: Boolean = false,
)

@Entity(
    tableName = "practice_item",
    primaryKeys = ["sessionId", "ordinal"],
    foreignKeys = [ForeignKey(
        entity = PracticeSession::class,
        parentColumns = ["sessionId"],
        childColumns = ["sessionId"],
    )],
)
internal data class PracticeItem(
    val sessionId: Long,
    val ordinal: Int,
    val wordId: String,
    val questionType: QuestionType? = null,
    val learnCompletedDay: Long? = null,
    val firstOutcome: Outcome? = null,
    val firstResolvedDay: Long? = null,
    val firstNextDueDay: Long? = null,
    val retryOrdinal: Int? = null,
    val retryOutcome: Outcome? = null,
)

@Dao
internal interface PracticeDao {
    @Query("SELECT * FROM learned_word")
    suspend fun learnedWords(): List<LearnedWord>

    @Query("SELECT * FROM review_schedule")
    suspend fun schedules(): List<ReviewSchedule>

    @Query("SELECT * FROM review_schedule WHERE wordId = :wordId AND questionType = :type")
    suspend fun schedule(wordId: String, type: QuestionType): ReviewSchedule

    @Query("SELECT * FROM practice_session WHERE openSlot = 1")
    suspend fun openSession(): PracticeSession?

    @Query("SELECT * FROM practice_item WHERE sessionId = :sessionId ORDER BY ordinal")
    suspend fun items(sessionId: Long): List<PracticeItem>

    @Query("SELECT EXISTS(SELECT 1 FROM practice_session WHERE finishedDay = :day)")
    suspend fun finishedOn(day: Long): Boolean

    @Insert suspend fun insertLearned(word: LearnedWord)
    @Insert suspend fun insertSchedules(schedules: List<ReviewSchedule>)
    @Insert suspend fun insertSession(session: PracticeSession): Long
    @Insert suspend fun insertItems(items: List<PracticeItem>)
    @Update suspend fun updateSchedule(schedule: ReviewSchedule)
    @Update suspend fun updateSession(session: PracticeSession)
    @Update suspend fun updateItem(item: PracticeItem)
    @Update suspend fun updateItems(items: List<PracticeItem>)
}

@Database(
    entities = [LearnedWord::class, ReviewSchedule::class, PracticeSession::class, PracticeItem::class],
    version = 1,
    exportSchema = true,
)
internal abstract class PracticeDatabase : RoomDatabase() {
    abstract fun practiceDao(): PracticeDao

    companion object {
        private val lock = Any()
        private var instance: PracticeDatabase? = null

        fun get(context: Context): PracticeDatabase = synchronized(lock) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, PracticeDatabase::class.java, "viaword.db",
            ).build().also { instance = it }
        }
    }
}
