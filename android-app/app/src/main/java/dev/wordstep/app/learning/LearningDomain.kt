package dev.wordstep.app.learning

data class VocabularyWord(
    val id: String,
    val term: String,
    val meaning: String,
    val example: String,
    val topic: String,
    val difficulty: String,
)

data class WordProgress(
    val wordId: String,
    val nextReviewAt: Long,
    val successStreak: Int,
    val attempts: Int,
    val lastSeenAt: Long,
)

data class SessionWord(
    val word: VocabularyWord,
    val isReview: Boolean,
)

object LearningDomain {
    private const val HOUR_MILLIS = 60 * 60 * 1000L
    private const val DAY_MILLIS = 24 * HOUR_MILLIS
    private val successIntervals = listOf(1, 3, 7, 14, 30)

    fun selectSession(
        words: List<VocabularyWord>,
        progress: List<WordProgress>,
        topic: String,
        newWordCount: Int,
        now: Long,
    ): List<SessionWord> {
        val progressByWord = progress.associateBy(WordProgress::wordId)
        val reviews = words
            .mapNotNull { word -> progressByWord[word.id]?.takeIf { it.nextReviewAt <= now }?.let { word to it } }
            .sortedBy { (_, record) -> record.nextReviewAt }
            .map { (word, _) -> SessionWord(word, isReview = true) }

        val newWords = words
            .asSequence()
            .filter { it.topic == topic && it.id !in progressByWord }
            .sortedWith(compareBy<VocabularyWord>({ difficultyRank(it.difficulty) }, { it.term }))
            .take(newWordCount.coerceAtLeast(0))
            .map { SessionWord(it, isReview = false) }
            .toList()

        return reviews + newWords
    }

    fun nextProgress(
        previous: WordProgress?,
        wordId: String,
        usedIt: Boolean,
        now: Long,
    ): WordProgress {
        val successStreak = if (usedIt) (previous?.successStreak ?: 0) + 1 else 0
        val delay = if (usedIt) {
            val intervalIndex = (successStreak - 1).coerceIn(0, successIntervals.lastIndex)
            successIntervals[intervalIndex] * DAY_MILLIS
        } else {
            8 * HOUR_MILLIS
        }

        return WordProgress(
            wordId = wordId,
            nextReviewAt = now + delay,
            successStreak = successStreak,
            attempts = (previous?.attempts ?: 0) + 1,
            lastSeenAt = now,
        )
    }

    private fun difficultyRank(difficulty: String): Int = when (difficulty.lowercase()) {
        "easy" -> 0
        "medium" -> 1
        else -> 2
    }
}

val vocabularyCatalog = listOf(
    VocabularyWord("cautious", "cautious", "Careful to avoid danger or mistakes.", "I was cautious crossing the busy road.", "Everyday", "easy"),
    VocabularyWord("curious", "curious", "Eager to know or learn something.", "Maya was curious about the old map.", "Everyday", "easy"),
    VocabularyWord("exhausted", "exhausted", "Very tired after using a lot of energy.", "After the long match, we were exhausted.", "Everyday", "easy"),
    VocabularyWord("generous", "generous", "Happy to give or share with others.", "Leo was generous and shared his snacks.", "Everyday", "easy"),
    VocabularyWord("hesitate", "hesitate", "To pause because you are unsure.", "I began to hesitate before answering.", "Everyday", "easy"),
    VocabularyWord("peculiar", "peculiar", "Unusual in a way that is interesting or strange.", "A peculiar sound came from the cupboard.", "Stories", "easy"),
    VocabularyWord("vanish", "vanish", "To disappear suddenly or completely.", "The rabbit seemed to vanish into the tall grass.", "Stories", "easy"),
    VocabularyWord("glimpse", "glimpse", "A quick look at something.", "We caught a glimpse of the fox at dusk.", "Nature", "easy"),
    VocabularyWord("astonished", "astonished", "Very surprised by something unexpected.", "I was astonished when the tiny seed grew overnight.", "Feelings", "medium"),
    VocabularyWord("determined", "determined", "Having made a firm decision not to give up.", "She was determined to finish the puzzle.", "Everyday", "medium"),
    VocabularyWord("reluctant", "reluctant", "Not really wanting to do something.", "Ben was reluctant to leave the den he had built.", "Feelings", "medium"),
    VocabularyWord("persuade", "persuade", "To help someone agree by giving reasons.", "We tried to persuade Dad to visit the museum.", "Everyday", "medium"),
    VocabularyWord("fortunate", "fortunate", "Lucky because something good happened.", "We were fortunate to find our lost dog.", "Everyday", "medium"),
    VocabularyWord("fragile", "fragile", "Easily broken or damaged.", "The fragile shell rested in the sand.", "Nature", "medium"),
    VocabularyWord("scarce", "scarce", "Hard to find or not available in large amounts.", "Water became scarce during the dry summer.", "Nature", "medium"),
    VocabularyWord("observe", "observe", "To watch carefully and notice details.", "The class went outside to observe the birds.", "School", "medium"),
    VocabularyWord("bewildered", "bewildered", "Confused because something is difficult to understand.", "I felt bewildered by the surprising instructions.", "Feelings", "difficult"),
    VocabularyWord("resilient", "resilient", "Able to recover and keep going after a setback.", "The resilient team practised again after losing.", "Everyday", "difficult"),
    VocabularyWord("meticulous", "meticulous", "Very careful about every small detail.", "He was meticulous when checking his science notes.", "School", "difficult"),
    VocabularyWord("inevitable", "inevitable", "Certain to happen and impossible to avoid.", "With dark clouds overhead, rain seemed inevitable.", "Nature", "difficult"),
    VocabularyWord("compassionate", "compassionate", "Kind and caring when someone is having a hard time.", "A compassionate friend listened while I talked.", "Feelings", "difficult"),
    VocabularyWord("vivid", "vivid", "So clear and detailed that it is easy to picture.", "The book gave a vivid description of the jungle.", "Stories", "difficult"),
    VocabularyWord("dilemma", "dilemma", "A difficult choice between two possibilities.", "I faced a dilemma: help my friend or finish my project.", "School", "difficult"),
    VocabularyWord("plausible", "plausible", "Seeming reasonable or likely to be true.", "Her explanation sounded plausible to the class.", "School", "difficult"),
    VocabularyWord("mischievous", "mischievous", "Playfully causing small trouble.", "The mischievous puppy stole one of my socks.", "Stories", "medium"),
    VocabularyWord("astonishing", "astonishing", "So surprising that it is hard to believe.", "The magician made an astonishing card trick.", "Stories", "medium"),
    VocabularyWord("delicate", "delicate", "Made or done with care because it can be easily damaged.", "She carried the delicate model with both hands.", "School", "easy"),
    VocabularyWord("enormous", "enormous", "Much bigger than usual.", "An enormous oak tree shaded the playground.", "Nature", "easy"),
    VocabularyWord("mutter", "mutter", "To speak quietly so it is difficult to hear.", "He began to mutter while searching for his pencil.", "Everyday", "easy"),
    VocabularyWord("resourceful", "resourceful", "Good at finding clever ways to solve problems.", "The resourceful campers built a shelter from branches.", "Stories", "difficult"),
)