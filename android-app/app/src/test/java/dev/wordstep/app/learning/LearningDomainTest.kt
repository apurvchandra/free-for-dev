package dev.wordstep.app.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningDomainTest {
    @Test
    fun dueReviewsComeBeforeNewWordsAndIgnoreTheSelectedTopic() {
        val words = listOf(
            word("review", "Stories"),
            word("new-school", "School"),
            word("future-review", "Everyday"),
        )
        val progress = listOf(
            WordProgress("review", nextReviewAt = 10, successStreak = 1, attempts = 1, lastSeenAt = 0),
            WordProgress("future-review", nextReviewAt = 200, successStreak = 1, attempts = 1, lastSeenAt = 0),
        )

        val session = LearningDomain.selectSession(words, progress, "School", 1, now = 100)

        assertEquals(listOf("review", "new-school"), session.map { it.word.id })
        assertTrue(session.first().isReview)
        assertFalse(session.last().isReview)
    }

    @Test
    fun sessionHonorsTheConfiguredNewWordCount() {
        val words = (1..6).map { word("school-$it", "School") }

        val session = LearningDomain.selectSession(words, emptyList(), "School", 3, now = 100)

        assertEquals(3, session.size)
        assertTrue(session.none { it.isReview })
    }

    @Test
    fun selfCheckSchedulesReviewAndTracksPracticeAgain() {
        val firstSuccess = LearningDomain.nextProgress(null, "word", usedIt = true, now = 0)
        val needsPractice = LearningDomain.nextProgress(firstSuccess, "word", usedIt = false, now = 100)

        assertEquals(1, firstSuccess.successStreak)
        assertEquals(24 * 60 * 60 * 1000L, firstSuccess.nextReviewAt)
        assertEquals(0, needsPractice.successStreak)
        assertEquals(2, needsPractice.attempts)
        assertEquals(100 + 8 * 60 * 60 * 1000L, needsPractice.nextReviewAt)
    }

    private fun word(id: String, topic: String) = VocabularyWord(
        id = id,
        term = id,
        meaning = "meaning",
        example = "example",
        topic = topic,
        difficulty = "easy",
    )
}