package dev.wordstep.app.ai

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import dev.wordstep.app.learning.VocabularyWord

enum class NanoAvailability {
    AVAILABLE,
    DOWNLOADABLE,
    DOWNLOADING,
    UNAVAILABLE,
}

class NanoPromptGenerator {
    private val model by lazy { Generation.getClient() }

    suspend fun checkAvailability(): NanoAvailability = when (model.checkStatus()) {
        FeatureStatus.AVAILABLE -> NanoAvailability.AVAILABLE
        FeatureStatus.DOWNLOADABLE -> NanoAvailability.DOWNLOADABLE
        FeatureStatus.DOWNLOADING -> NanoAvailability.DOWNLOADING
        else -> NanoAvailability.UNAVAILABLE
    }

    suspend fun download(onAvailabilityChanged: (NanoAvailability) -> Unit) {
        model.download().collect { status ->
            when (status) {
                is com.google.mlkit.genai.common.DownloadStatus.DownloadStarted ->
                    onAvailabilityChanged(NanoAvailability.DOWNLOADING)
                is com.google.mlkit.genai.common.DownloadStatus.DownloadProgress ->
                    onAvailabilityChanged(NanoAvailability.DOWNLOADING)
                com.google.mlkit.genai.common.DownloadStatus.DownloadCompleted ->
                    onAvailabilityChanged(NanoAvailability.AVAILABLE)
                is com.google.mlkit.genai.common.DownloadStatus.DownloadFailed ->
                    onAvailabilityChanged(NanoAvailability.UNAVAILABLE)
            }
        }
    }

    suspend fun createConversationPrompt(word: VocabularyWord): String? {
        if (checkAvailability() != NanoAvailability.AVAILABLE) return null
        val safeTerm = word.term.filter(Char::isLetter).take(36)
        if (safeTerm.isBlank()) return null

        val response = model.generateContent(
            "Create one short, kind, age-appropriate conversation question for a 10-year-old. " +
                "Invite them to use the vocabulary word '$safeTerm' naturally. " +
                "Do not define the word, request personal information, or add a second sentence. " +
                "Return only the question, in 18 words or fewer.",
        )
        val suggestion = response.candidates.firstOrNull()?.text
            ?.trim()?.lineSequence()?.firstOrNull().orEmpty()
        return suggestion.takeIf {
            it.length in 12..180 && safeTerm.containsMatchIn(it)
        }
    }
}

private fun String.containsMatchIn(value: String): Boolean =
    value.contains(this, ignoreCase = true)