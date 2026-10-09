package dev.wordstep.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.wordstep.app.ai.NanoAvailability
import dev.wordstep.app.ai.NanoPromptGenerator
import dev.wordstep.app.data.LearningRepository
import dev.wordstep.app.data.LearnerProfileEntity
import dev.wordstep.app.data.WordstepDatabase
import dev.wordstep.app.learning.SessionWord
import dev.wordstep.app.learning.VocabularyWord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WordstepScreen {
    HOME,
    PRACTICE,
    COMPLETE,
}

data class WordstepUiState(
    val isLoading: Boolean = true,
    val profiles: List<LearnerProfileEntity> = emptyList(),
    val selectedProfileId: Long? = null,
    val selectedTopic: String = "Everyday",
    val wordsPerSession: Int = 5,
    val dueReviewCount: Int = 0,
    val learnedCount: Int = 0,
    val screen: WordstepScreen = WordstepScreen.HOME,
    val session: List<SessionWord> = emptyList(),
    val currentIndex: Int = 0,
    val usedCount: Int = 0,
    val retryCount: Int = 0,
    val prompt: String = "",
    val isGeneratingPrompt: Boolean = false,
    val nanoAvailability: NanoAvailability = NanoAvailability.UNAVAILABLE,
    val message: String? = null,
) {
    val selectedProfile: LearnerProfileEntity?
        get() = profiles.firstOrNull { it.id == selectedProfileId }

    val currentWord: SessionWord?
        get() = session.getOrNull(currentIndex)
}

class WordstepViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LearningRepository(WordstepDatabase.get(application))
    private val nanoPromptGenerator = NanoPromptGenerator()
    private val mutableState = MutableStateFlow(WordstepUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.prepare()
            repository.observeProfiles().collect { profiles ->
                val previous = mutableState.value
                val selected = profiles.firstOrNull { it.id == previous.selectedProfileId }
                    ?: profiles.firstOrNull()
                mutableState.update {
                    it.copy(
                        isLoading = false,
                        profiles = profiles,
                        selectedProfileId = selected?.id,
                        wordsPerSession = selected?.wordsPerSession ?: 5,
                    )
                }
                selected?.let { refreshProgress(it.id) }
            }
        }
        viewModelScope.launch {
            val availability = runCatching { nanoPromptGenerator.checkAvailability() }
                .getOrDefault(NanoAvailability.UNAVAILABLE)
            mutableState.update { it.copy(nanoAvailability = availability) }
        }
    }

    fun selectProfile(profileId: Long) {
        val profile = mutableState.value.profiles.firstOrNull { it.id == profileId } ?: return
        mutableState.update {
            it.copy(
                selectedProfileId = profileId,
                wordsPerSession = profile.wordsPerSession,
                message = null,
            )
        }
        viewModelScope.launch { refreshProgress(profileId) }
    }

    fun createProfile(name: String) {
        val cleanName = name.trim().take(30)
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            val profileId = repository.createProfile(cleanName)
            mutableState.update {
                it.copy(selectedProfileId = profileId, wordsPerSession = 5, message = null)
            }
        }
    }

    fun renameSelectedProfile(name: String) {
        val profileId = mutableState.value.selectedProfileId ?: return
        val cleanName = name.trim().take(30)
        if (cleanName.isBlank()) return
        viewModelScope.launch { repository.renameProfile(profileId, cleanName) }
    }

    fun setWordCount(count: Int) {
        val profile = mutableState.value.selectedProfile ?: return
        if (count !in listOf(3, 5, 10)) return
        mutableState.update { it.copy(wordsPerSession = count) }
        viewModelScope.launch { repository.updateSessionSize(profile, count) }
    }

    fun selectTopic(topic: String) {
        mutableState.update { it.copy(selectedTopic = topic, message = null) }
    }

    fun addCustomWord(term: String, meaning: String, example: String, topic: String) {
        val cleanTerm = term.trim().take(40)
        val cleanMeaning = meaning.trim().take(180)
        val cleanExample = example.trim().take(180)
        if (cleanTerm.isBlank() || cleanMeaning.isBlank() || cleanExample.isBlank()) return
        val id = "custom-${cleanTerm.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')}"
        val word = VocabularyWord(id, cleanTerm, cleanMeaning, cleanExample, topic, "medium")
        viewModelScope.launch {
            val saved = repository.addCustomWord(word)
            mutableState.update {
                it.copy(message = if (saved) "Word added to your list." else "That word is already in the list.")
            }
        }
    }

    fun startSession() {
        val current = mutableState.value
        val profileId = current.selectedProfileId ?: return
        viewModelScope.launch {
            val words = repository.buildSession(
                profileId = profileId,
                topic = current.selectedTopic,
                newWordCount = current.wordsPerSession,
            )
            if (words.isEmpty()) {
                mutableState.update { it.copy(message = "No new words in this topic yet. Try another topic.") }
                return@launch
            }
            mutableState.update {
                it.copy(
                    screen = WordstepScreen.PRACTICE,
                    session = words,
                    currentIndex = 0,
                    usedCount = 0,
                    retryCount = 0,
                    prompt = curatedPrompt(words.first().word),
                    message = null,
                )
            }
        }
    }

    fun markCurrentWord(usedIt: Boolean) {
        val current = mutableState.value
        val selected = current.currentWord ?: return
        val profileId = current.selectedProfileId ?: return
        viewModelScope.launch {
            repository.saveSelfCheck(profileId, selected, usedIt)
            val nextIndex = current.currentIndex + 1
            val completed = nextIndex >= current.session.size
            mutableState.update {
                it.copy(
                    screen = if (completed) WordstepScreen.COMPLETE else WordstepScreen.PRACTICE,
                    currentIndex = nextIndex,
                    usedCount = it.usedCount + if (usedIt) 1 else 0,
                    retryCount = it.retryCount + if (!usedIt) 1 else 0,
                    prompt = if (completed) "" else curatedPrompt(it.session[nextIndex].word),
                    isGeneratingPrompt = false,
                )
            }
            refreshProgress(profileId)
        }
    }

    fun makeAnotherPrompt() {
        val word = mutableState.value.currentWord?.word ?: return
        mutableState.update { it.copy(isGeneratingPrompt = true, message = null) }
        viewModelScope.launch {
            val generated = runCatching { nanoPromptGenerator.createConversationPrompt(word) }.getOrNull()
            mutableState.update {
                it.copy(
                    prompt = generated ?: curatedPrompt(word),
                    isGeneratingPrompt = false,
                    message = if (generated == null) "Using a ready-made idea for this word." else null,
                )
            }
        }
    }

    fun prepareNano() {
        mutableState.update { it.copy(nanoAvailability = NanoAvailability.DOWNLOADING, message = null) }
        viewModelScope.launch {
            runCatching {
                nanoPromptGenerator.download { availability ->
                    mutableState.update { it.copy(nanoAvailability = availability) }
                }
            }
            val availability = runCatching { nanoPromptGenerator.checkAvailability() }
                .getOrDefault(NanoAvailability.UNAVAILABLE)
            mutableState.update { it.copy(nanoAvailability = availability) }
        }
    }

    fun returnHome() {
        mutableState.update { it.copy(screen = WordstepScreen.HOME, message = null) }
    }

    private suspend fun refreshProgress(profileId: Long) {
        val dueCount = repository.dueCount(profileId)
        val learnedCount = repository.learnedCount(profileId)
        mutableState.update { it.copy(dueReviewCount = dueCount, learnedCount = learnedCount) }
    }

    private fun curatedPrompt(word: VocabularyWord): String = when (word.topic) {
        "Stories" -> "Imagine a character who is ${word.term}. What might they do?"
        "Feelings" -> "When might someone feel ${word.term}? Tell me about a made-up moment."
        "School" -> "Can you use ${word.term} to describe something you learned or noticed?"
        "Nature" -> "Where might you spot something ${word.term} outdoors?"
        else -> "Can you use ${word.term} to describe something from today?"
    }
}