package dev.wordstep.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import dev.wordstep.app.learning.VocabularyWord
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "learner_profiles")
data class LearnerProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val age: Int = 10,
    val wordsPerSession: Int = 5,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "vocabulary_words")
data class VocabularyWordEntity(
    @PrimaryKey val id: String,
    val term: String,
    val meaning: String,
    val example: String,
    val topic: String,
    val difficulty: String,
    val isCustom: Boolean = false,
) {
    fun toDomain() = VocabularyWord(id, term, meaning, example, topic, difficulty)

    companion object {
        fun fromDomain(word: VocabularyWord) = VocabularyWordEntity(
            id = word.id,
            term = word.term,
            meaning = word.meaning,
            example = word.example,
            topic = word.topic,
            difficulty = word.difficulty,
        )
    }
}

@Entity(
    tableName = "word_progress",
    primaryKeys = ["profileId", "wordId"],
    foreignKeys = [
        ForeignKey(
            entity = LearnerProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = VocabularyWordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("wordId")],
)
data class WordProgressEntity(
    val profileId: Long,
    val wordId: String,
    val nextReviewAt: Long,
    val successStreak: Int,
    val attempts: Int,
    val lastSeenAt: Long,
)

@Dao
interface LearnerProfileDao {
    @Query("SELECT * FROM learner_profiles ORDER BY createdAt")
    fun observeAll(): Flow<List<LearnerProfileEntity>>

    @Query("SELECT COUNT(*) FROM learner_profiles")
    suspend fun count(): Int

    @Insert
    suspend fun insert(profile: LearnerProfileEntity): Long

    @Upsert
    suspend fun upsert(profile: LearnerProfileEntity)

    @Query("UPDATE learner_profiles SET name = :name WHERE id = :profileId")
    suspend fun rename(profileId: Long, name: String)

    @Delete
    suspend fun delete(profile: LearnerProfileEntity)
}

@Dao
interface VocabularyWordDao {
    @Query("SELECT * FROM vocabulary_words ORDER BY term")
    suspend fun getAll(): List<VocabularyWordEntity>

    @Query("SELECT * FROM vocabulary_words WHERE id = :wordId")
    suspend fun getById(wordId: String): VocabularyWordEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<VocabularyWordEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(word: VocabularyWordEntity): Long
}

@Dao
interface WordProgressDao {
    @Query("SELECT * FROM word_progress WHERE profileId = :profileId")
    suspend fun getForProfile(profileId: Long): List<WordProgressEntity>

    @Query("SELECT * FROM word_progress WHERE profileId = :profileId AND wordId = :wordId")
    suspend fun get(profileId: Long, wordId: String): WordProgressEntity?

    @Upsert
    suspend fun upsert(progress: WordProgressEntity)
}

@Database(
    entities = [LearnerProfileEntity::class, VocabularyWordEntity::class, WordProgressEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class WordstepDatabase : RoomDatabase() {
    abstract fun learnerProfiles(): LearnerProfileDao
    abstract fun vocabularyWords(): VocabularyWordDao
    abstract fun wordProgress(): WordProgressDao

    companion object {
        @Volatile
        private var instance: WordstepDatabase? = null

        fun get(context: Context): WordstepDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                WordstepDatabase::class.java,
                "wordstep.db",
            ).build().also { instance = it }
        }
    }
}