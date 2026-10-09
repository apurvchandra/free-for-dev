package dev.wordstep.app.data

import androidx.room.withTransaction
import dev.wordstep.app.learning.LearningDomain
import dev.wordstep.app.learning.SessionWord
import dev.wordstep.app.learning.WordProgress
import dev.wordstep.app.learning.vocabularyCatalog
import kotlinx.coroutines.flow.Flow

class LearningRepository(private val database: WordstepDatabase) {
    private val profiles = database.learnerProfiles()
    private val words = database.vocabularyWords()
    private val progress = database.wordProgress()

    fun observeProfiles(): Flow<List<LearnerProfileEntity>> = profiles.observeAll()

    suspend fun prepare() {
        database.withTransaction {
            words.insertAll(vocabularyCatalog.map(VocabularyWordEntity::fromDomain))
            if (profiles.count() == 0) {
                profiles.insert(LearnerProfileEntity(name = "My learner"))
            }
        }
    }

    suspend fun createProfile(name: String): Long = profiles.insert(
        LearnerProfileEntity(name = name.trim().ifBlank { "New learner" }),
    )

    suspend fun updateSessionSize(profile: LearnerProfileEntity, count: Int) {
        profiles.upsert(profile.copy(wordsPerSession = count))
    }

    suspend fun renameProfile(profileId: Long, name: String) {
        profiles.rename(profileId, name.trim())
    }

    suspend fun deleteProfile(profile: LearnerProfileEntity) {
        profiles.delete(profile)
    }

    suspend fun addCustomWord(word: dev.wordstep.app.learning.VocabularyWord): Boolean {
        return words.insert(VocabularyWordEntity.fromDomain(word)) != -1L
    }

    suspend fun buildSession(
        profileId: Long,
        topic: String,
        newWordCount: Int,
        now: Long = System.currentTimeMillis(),
    ): List<SessionWord> {
        val allWords = words.getAll().map(VocabularyWordEntity::toDomain)
        val profileProgress = progress.getForProfile(profileId).map {
            WordProgress(it.wordId, it.nextReviewAt, it.successStreak, it.attempts, it.lastSeenAt)
        }
        return LearningDomain.selectSession(allWords, profileProgress, topic, newWordCount, now)
    }

    suspend fun saveSelfCheck(profileId: Long, word: SessionWord, usedIt: Boolean) {
        val previous = progress.get(profileId, word.word.id)?.let {
            WordProgress(it.wordId, it.nextReviewAt, it.successStreak, it.attempts, it.lastSeenAt)
        }
        val next = LearningDomain.nextProgress(previous, word.word.id, usedIt, System.currentTimeMillis())
        progress.upsert(
            WordProgressEntity(
                profileId = profileId,
                wordId = next.wordId,
                nextReviewAt = next.nextReviewAt,
                successStreak = next.successStreak,
                attempts = next.attempts,
                lastSeenAt = next.lastSeenAt,
            ),
        )
    }

    suspend fun dueCount(profileId: Long, now: Long = System.currentTimeMillis()): Int =
        progress.getForProfile(profileId).count { it.nextReviewAt <= now }

    suspend fun learnedCount(profileId: Long): Int = progress.getForProfile(profileId).size
}