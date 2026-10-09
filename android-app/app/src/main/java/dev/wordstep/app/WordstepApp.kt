package dev.wordstep.app

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.wordstep.app.ai.NanoAvailability
import dev.wordstep.app.data.LearnerProfileEntity
import dev.wordstep.app.learning.SessionWord

private val Forest = Color(0xFF175C56)
private val Ink = Color(0xFF20333A)
private val MutedInk = Color(0xFF617278)
private val Leaf = Color(0xFFDDEEE5)
private val Coral = Color(0xFFE77B64)
private val Sun = Color(0xFFF0C65E)
private val Sky = Color(0xFFDCEBF4)
private val Canvas = Color(0xFFF5F8F5)

private val WordstepColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    secondary = Coral,
    onSecondary = Color.White,
    tertiary = Sun,
    background = Canvas,
    surface = Color.White,
    onSurface = Ink,
    onBackground = Ink,
)

private val Topics = listOf("Everyday", "Stories", "Feelings", "School", "Nature")
private val WordCounts = listOf(3, 5, 10)

@Composable
fun WordstepApp(viewModel: WordstepViewModel) {
    val state by viewModel.state.collectAsState()
    MaterialTheme(colorScheme = WordstepColors) {
        Surface(modifier = Modifier.fillMaxSize(), color = Canvas) {
            when {
                state.isLoading -> LoadingScreen()
                state.screen == WordstepScreen.PRACTICE -> PracticeScreen(state, viewModel)
                state.screen == WordstepScreen.COMPLETE -> CompleteScreen(state, viewModel)
                else -> HomeScreen(state, viewModel)
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Forest)
    }
}

@Composable
private fun HomeScreen(state: WordstepUiState, viewModel: WordstepViewModel) {
    var showAddWord by rememberSaveable { mutableStateOf(false) }
    var profileDialog by rememberSaveable { mutableStateOf<ProfileDialog?>(null) }

    Scaffold(
        containerColor = Canvas,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(padding)
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("wordstep", color = Forest, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                    Text("A little practice, a lot to say.", color = MutedInk, fontSize = 14.sp)
                }
                ProfilePicker(
                    profiles = state.profiles,
                    selectedProfile = state.selectedProfile,
                    onSelect = viewModel::selectProfile,
                    onAdd = { profileDialog = ProfileDialog.Add },
                    onRename = { profile -> profileDialog = ProfileDialog.Rename(profile.name) },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBlock(
                    value = state.dueReviewCount.toString(),
                    label = "to revisit",
                    color = Sky,
                    modifier = Modifier.weight(1f),
                )
                StatBlock(
                    value = state.learnedCount.toString(),
                    label = "words tried",
                    color = Leaf,
                    modifier = Modifier.weight(1f),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Pick a conversation")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Topics.forEach { topic ->
                        FilterChip(
                            selected = state.selectedTopic == topic,
                            onClick = { viewModel.selectTopic(topic) },
                            label = { Text(topic) },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("New words")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    WordCounts.forEachIndexed { index, count ->
                        SegmentedButton(
                            selected = state.wordsPerSession == count,
                            onClick = { viewModel.setWordCount(count) },
                            shape = SegmentedButtonDefaults.itemShape(index, WordCounts.size),
                            label = { Text("$count") },
                        )
                    }
                }
                Text(
                    "Words waiting for review are added automatically.",
                    color = MutedInk,
                    fontSize = 13.sp,
                )
            }

            Button(
                onClick = viewModel::startSession,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Forest),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Start today’s words", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { showAddWord = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add a word of your own")
            }

            NanoStatus(state.nanoAvailability, viewModel::prepareNano)

            state.message?.let { MessageStrip(it) }
        }
    }

    if (showAddWord) {
        AddWordDialog(
            onDismiss = { showAddWord = false },
            onSave = { term, meaning, example ->
                viewModel.addCustomWord(term, meaning, example, state.selectedTopic)
                showAddWord = false
            },
        )
    }
    profileDialog?.let { dialog ->
        ProfileEditorDialog(
            dialog = dialog,
            onDismiss = { profileDialog = null },
            onSave = { name ->
                when (dialog) {
                    ProfileDialog.Add -> viewModel.createProfile(name)
                    is ProfileDialog.Rename -> viewModel.renameSelectedProfile(name)
                }
                profileDialog = null
            },
        )
    }
}

@Composable
private fun ProfilePicker(
    profiles: List<LearnerProfileEntity>,
    selectedProfile: LearnerProfileEntity?,
    onSelect: (Long) -> Unit,
    onAdd: () -> Unit,
    onRename: (LearnerProfileEntity) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(selectedProfile?.name ?: "Learner", maxLines = 1)
            Icon(Icons.Default.ExpandMore, contentDescription = "Choose learner profile")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            profiles.forEach { profile ->
                DropdownMenuItem(
                    text = { Text(profile.name) },
                    onClick = {
                        onSelect(profile.id)
                        expanded = false
                    },
                    trailingIcon = if (profile.id == selectedProfile?.id) {
                        { Icon(Icons.Default.Check, contentDescription = "Selected") }
                    } else null,
                )
            }
            if (profiles.isNotEmpty()) HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Rename profile") },
                onClick = {
                    selectedProfile?.let(onRename)
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text("Add learner") },
                onClick = {
                    onAdd()
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun StatBlock(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
            Text(value, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(label, fontSize = 13.sp, color = Ink)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
}

@Composable
private fun NanoStatus(availability: NanoAvailability, onPrepare: () -> Unit) {
    when (availability) {
        NanoAvailability.AVAILABLE -> StatusLine("On-device prompt ideas are ready", Leaf)
        NanoAvailability.DOWNLOADABLE -> Surface(color = Sky, shape = RoundedCornerShape(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("On-device ideas can be prepared", color = Ink, fontSize = 13.sp)
                TextButton(onClick = onPrepare) { Text("Prepare") }
            }
        }
        NanoAvailability.DOWNLOADING -> StatusLine("Preparing on-device ideas…", Sky)
        NanoAvailability.UNAVAILABLE -> StatusLine("Ready-made prompts work on every device.", Leaf)
    }
}

@Composable
private fun StatusLine(text: String, color: Color) {
    Surface(color = color, shape = RoundedCornerShape(8.dp)) {
        Text(text, modifier = Modifier.fillMaxWidth().padding(13.dp), color = Ink, fontSize = 13.sp)
    }
}

@Composable
private fun MessageStrip(text: String) {
    Text(text, color = Forest, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun PracticeScreen(state: WordstepUiState, viewModel: WordstepViewModel) {
    val sessionWord = state.currentWord ?: return
    Scaffold(containerColor = Canvas) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = 22.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::returnHome) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Leave this session")
                }
                Text("Today’s words", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                Text("${state.currentIndex + 1} / ${state.session.size}", color = MutedInk)
            }
            LinearProgressIndicator(
                progress = { (state.currentIndex + 1).toFloat() / state.session.size },
                modifier = Modifier.fillMaxWidth().padding(top = 7.dp, bottom = 15.dp),
                color = Coral,
                trackColor = Leaf,
            )

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    LabelTag(if (sessionWord.isReview) "REVIEW" else "NEW", if (sessionWord.isReview) Sky else Leaf)
                    LabelTag(sessionWord.word.difficulty.uppercase(), Sun.copy(alpha = 0.55f))
                    Text(sessionWord.word.topic, color = MutedInk, fontSize = 12.sp)
                }
                Text(sessionWord.word.term, color = Forest, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("MEANING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MutedInk)
                    Text(sessionWord.word.meaning, fontSize = 18.sp, lineHeight = 26.sp, color = Ink)
                }
                Surface(color = Color.White, shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Coral, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("IN A SENTENCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MutedInk)
                            Spacer(Modifier.height(4.dp))
                            Text("“${sessionWord.word.example}”", fontSize = 16.sp, lineHeight = 23.sp, color = Ink)
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("YOUR TURN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MutedInk)
                    Text(state.prompt, fontSize = 18.sp, lineHeight = 26.sp, color = Ink)
                    Text("Say your sentence aloud, then choose how it went.", fontSize = 13.sp, color = MutedInk)
                    if (state.nanoAvailability == NanoAvailability.AVAILABLE) {
                        TextButton(onClick = viewModel::makeAnotherPrompt, enabled = !state.isGeneratingPrompt) {
                            if (state.isGeneratingPrompt) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            }
                            Spacer(Modifier.width(7.dp))
                            Text("Try another idea")
                        }
                    }
                }
                state.message?.let { MessageStrip(it) }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { viewModel.markCurrentWord(usedIt = false) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Try again")
                }
                Button(
                    onClick = { viewModel.markCurrentWord(usedIt = true) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Forest),
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("I used it")
                }
            }
        }
    }
}

@Composable
private fun LabelTag(text: String, color: Color) {
    Surface(color = color, shape = RoundedCornerShape(5.dp)) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CompleteScreen(state: WordstepUiState, viewModel: WordstepViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Surface(color = Sun, shape = RoundedCornerShape(8.dp)) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.padding(12.dp).size(28.dp), tint = Ink)
        }
        Spacer(Modifier.height(24.dp))
        Text("Words ready for real life.", fontSize = 31.sp, lineHeight = 37.sp, fontWeight = FontWeight.ExtraBold, color = Forest)
        Spacer(Modifier.height(10.dp))
        Text(
            "You tried ${state.session.size} words and used ${state.usedCount} in your own sentences.",
            fontSize = 17.sp,
            lineHeight = 25.sp,
            color = Ink,
        )
        Spacer(Modifier.height(8.dp))
        Text("${state.retryCount} saved to practise again.", color = MutedInk)
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = viewModel::returnHome,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Forest),
        ) {
            Text("Back to home", fontWeight = FontWeight.Bold)
        }
    }
}

private sealed interface ProfileDialog {
    data object Add : ProfileDialog
    data class Rename(val currentName: String) : ProfileDialog
}

@Composable
private fun ProfileEditorDialog(dialog: ProfileDialog, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf((dialog as? ProfileDialog.Rename)?.currentName.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (dialog is ProfileDialog.Add) "Add a learner" else "Rename profile") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddWordDialog(
    onDismiss: () -> Unit,
    onSave: (term: String, meaning: String, example: String) -> Unit,
) {
    var term by rememberSaveable { mutableStateOf("") }
    var meaning by rememberSaveable { mutableStateOf("") }
    var example by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a word") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = term,
                    onValueChange = { term = it.take(40) },
                    label = { Text("Word") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                OutlinedTextField(
                    value = meaning,
                    onValueChange = { meaning = it.take(180) },
                    label = { Text("Meaning") },
                    minLines = 2,
                )
                OutlinedTextField(
                    value = example,
                    onValueChange = { example = it.take(180) },
                    label = { Text("Example sentence") },
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(term, meaning, example) },
                enabled = term.isNotBlank() && meaning.isNotBlank() && example.isNotBlank(),
            ) { Text("Add word") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}