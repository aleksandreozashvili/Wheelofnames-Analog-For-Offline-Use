package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.WheelEntity
import com.example.ui.WheelViewModel
import com.example.ui.WheelViewModelFactory
import com.example.ui.components.ConfettiView
import com.example.ui.components.WheelView
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val viewModel: WheelViewModel = viewModel(factory = WheelViewModelFactory(context))

    val names by viewModel.namesList.collectAsStateWithLifecycle()
    val isSpinning by viewModel.isSpinning.collectAsStateWithLifecycle()
    val winner by viewModel.winner.collectAsStateWithLifecycle()
    val showWinnerDialog by viewModel.showWinnerDialog.collectAsStateWithLifecycle()
    val theme by viewModel.colorTheme.collectAsStateWithLifecycle()
    val centerEmoji by viewModel.centerEmoji.collectAsStateWithLifecycle()
    val spinDuration by viewModel.spinDuration.collectAsStateWithLifecycle()
    val wheelTitle by viewModel.wheelTitle.collectAsStateWithLifecycle()
    val inputName by viewModel.inputName.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val savedWheels by viewModel.savedWheels.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }

    // Bottom sheet & dialog local states
    var showSavedWheelsSheet by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveDialogTitleInput by remember { mutableStateOf("") }
    var showEditTitleDialog by remember { mutableStateOf(false) }
    var editTitleInput by remember { mutableStateOf("") }

    // Synchronize rotation with changes in wheel state (resets to 0 for new wheels)
    LaunchedEffect(names.size) {
        if (!isSpinning && rotation.value != 0f) {
            // Keep rotation but align to clean bounds if needed
        }
    }

    // Core spin execution block
    fun runSpinAnimation() {
        if (names.isEmpty() || isSpinning) return
        viewModel.startSpinning()
        
        coroutineScope.launch {
            // Custom multi-rotation ease-out spin animation
            val extraRotations = (6..10).random() * 360f
            val randomOffset = (0..359).random().toFloat()
            val targetAngle = rotation.value + extraRotations + randomOffset
            
            rotation.animateTo(
                targetValue = targetAngle,
                animationSpec = tween(
                    durationMillis = spinDuration * 1000,
                    easing = CubicBezierEasing(0.12f, 0.8f, 0.15f, 1.0f) // Ultra smooth decelleration curve
                )
            ) {
                // Keep viewmodel rotation angle in sync to trigger tick sound effects
                viewModel.updateRotationAngle(this.value)
            }

            // Pinpoint the selected winning segment mathematically based on pointer at -90 degrees
            val sweepAngle = 360f / names.size
            val relativeAngle = (-90f - rotation.value) % 360f
            val positiveAngle = (relativeAngle + 360f) % 360f
            val winningIndex = (positiveAngle / sweepAngle).toInt() % names.size
            
            viewModel.finishSpinning(winningIndex)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                if (!isSpinning) {
                                    editTitleInput = wheelTitle
                                    showEditTitleDialog = true
                                }
                            }
                            .testTag("app_title_row")
                    ) {
                        Text(
                            text = wheelTitle,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Title",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                actions = {
                    // Sound state toggle
                    IconButton(
                        onClick = { viewModel.toggleSound() },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (soundEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.testTag("sound_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
                            contentDescription = "Toggle Sounds"
                        )
                    }

                    // Save current configuration
                    IconButton(
                        onClick = {
                            if (!isSpinning) {
                                saveDialogTitleInput = wheelTitle
                                showSaveDialog = true
                            }
                        },
                        enabled = !isSpinning,
                        modifier = Modifier.testTag("save_wheel_button")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save Wheel")
                    }

                    // Open offline wheels loader
                    IconButton(
                        onClick = { showSavedWheelsSheet = true },
                        modifier = Modifier.testTag("load_wheels_button")
                    ) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Saved Wheels")
                    }

                    // Reset / New wheel config
                    IconButton(
                        onClick = {
                            if (!isSpinning) {
                                viewModel.startNewWheel()
                                coroutineScope.launch { rotation.snapTo(0f) }
                            }
                        },
                        enabled = !isSpinning,
                        modifier = Modifier.testTag("new_wheel_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset New")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Adaptive Grid / Layout Engine (supports phones & landscape/tablet orientations)
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val screenWidth = maxWidth
                val screenHeight = maxHeight
                val isWideScreen = screenWidth > 600.dp

                if (isWideScreen) {
                    // Two column side-by-side layout for widescreen ergonomics
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Left column: Interactive spinning wheel
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(minOf(screenWidth * 0.45f, screenHeight * 0.7f))
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                WheelView(
                                    names = names,
                                    rotationAngle = rotation.value,
                                    theme = theme,
                                    centerEmoji = centerEmoji,
                                    modifier = Modifier.fillMaxSize(),
                                    soundSynthesizer = viewModel.soundSynthesizer,
                                    onTick = { viewModel.soundSynthesizer.playTick() }
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { runSpinAnimation() },
                                modifier = Modifier
                                    .widthIn(min = 200.dp)
                                    .height(54.dp)
                                    .testTag("spin_button"),
                                enabled = !isSpinning && names.isNotEmpty(),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSpinning) "SPINNING..." else "SPIN THE WHEEL",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        // Right column: Name editor & configuration tabs
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            ControlTabs(
                                names = names,
                                isSpinning = isSpinning,
                                inputName = inputName,
                                onInputNameChange = { viewModel.setInputName(it) },
                                onAddName = { viewModel.addName(it) },
                                onRemoveName = { viewModel.removeName(it) },
                                onClearNames = { viewModel.clearNames() },
                                onShuffle = { viewModel.shuffleNames() },
                                onSort = { viewModel.sortNames() },
                                selectedTheme = theme,
                                onThemeChange = { viewModel.updateTheme(it) },
                                centerEmoji = centerEmoji,
                                onEmojiChange = { viewModel.updateCenterEmoji(it) },
                                spinDuration = spinDuration,
                                onDurationChange = { viewModel.updateSpinDuration(it) }
                            )
                        }
                    }
                } else {
                    // Mobile portrait stacked layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Centered Wheel viewport
                        Box(
                            modifier = Modifier
                                .size(screenWidth * 0.82f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            WheelView(
                                names = names,
                                rotationAngle = rotation.value,
                                theme = theme,
                                centerEmoji = centerEmoji,
                                modifier = Modifier.fillMaxSize(),
                                soundSynthesizer = viewModel.soundSynthesizer,
                                onTick = { viewModel.soundSynthesizer.playTick() }
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Spin Trigger
                        Button(
                            onClick = { runSpinAnimation() },
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(52.dp)
                                .testTag("spin_button"),
                            enabled = !isSpinning && names.isNotEmpty(),
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSpinning) "SPINNING..." else "SPIN NOW!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Controls Container (Tab-based panels)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(460.dp),
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                            )
                        ) {
                            ControlTabs(
                                names = names,
                                isSpinning = isSpinning,
                                inputName = inputName,
                                onInputNameChange = { viewModel.setInputName(it) },
                                onAddName = { viewModel.addName(it) },
                                onRemoveName = { viewModel.removeName(it) },
                                onClearNames = { viewModel.clearNames() },
                                onShuffle = { viewModel.shuffleNames() },
                                onSort = { viewModel.sortNames() },
                                selectedTheme = theme,
                                onThemeChange = { viewModel.updateTheme(it) },
                                centerEmoji = centerEmoji,
                                onEmojiChange = { viewModel.updateCenterEmoji(it) },
                                spinDuration = spinDuration,
                                onDurationChange = { viewModel.updateSpinDuration(it) }
                            )
                        }
                    }
                }
            }

            // High performance local canvas confetti layer covering entire screen during celebration
            ConfettiView(isActive = showWinnerDialog, modifier = Modifier.fillMaxSize())

            // Winner dialog / Celebration alert
            if (showWinnerDialog && winner != null) {
                WinnerCelebrationDialog(
                    winnerName = winner!!,
                    onClose = { viewModel.closeWinnerDialog() },
                    // Heart of wheelofnames.com Progressive Elimination Raffle feature:
                    onRemove = { viewModel.removeWinnerAndClose() }
                )
            }

            // Sheet holding saved wheels list loaded offline
            if (showSavedWheelsSheet) {
                SavedWheelsBottomSheet(
                    wheels = savedWheels,
                    onClose = { showSavedWheelsSheet = false },
                    onLoadWheel = {
                        viewModel.loadWheel(it)
                        coroutineScope.launch { rotation.snapTo(0f) }
                        showSavedWheelsSheet = false
                    },
                    onDeleteWheel = { viewModel.deleteWheel(it) }
                )
            }

            // Save dialog input box
            if (showSaveDialog) {
                SaveWheelTitleDialog(
                    initialTitle = saveDialogTitleInput,
                    onClose = { showSaveDialog = false },
                    onSave = {
                        viewModel.saveWheel(it)
                        showSaveDialog = false
                    }
                )
            }

            // Edit current wheel title dialog
            if (showEditTitleDialog) {
                EditWheelTitleDialog(
                    initialTitle = editTitleInput,
                    onClose = { showEditTitleDialog = false },
                    onConfirm = {
                        viewModel.updateWheelTitle(it)
                        showEditTitleDialog = false
                    }
                )
            }
        }
    }
}

// Custom tab layout organizer for name listings and visual personalization parameters
@Composable
fun ControlTabs(
    names: List<String>,
    isSpinning: Boolean,
    inputName: String,
    onInputNameChange: (String) -> Unit,
    onAddName: (String) -> Unit,
    onRemoveName: (Int) -> Unit,
    onClearNames: () -> Unit,
    onShuffle: () -> Unit,
    onSort: () -> Unit,
    selectedTheme: String,
    onThemeChange: (String) -> Unit,
    centerEmoji: String,
    onEmojiChange: (String) -> Unit,
    spinDuration: Int,
    onDurationChange: (Int) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Names & Actions", "Personalize")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("control_tab_$index")
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> NamesPanel(
                    names = names,
                    isSpinning = isSpinning,
                    inputName = inputName,
                    onInputNameChange = onInputNameChange,
                    onAddName = onAddName,
                    onRemoveName = onRemoveName,
                    onClearNames = onClearNames,
                    onShuffle = onShuffle,
                    onSort = onSort
                )
                1 -> SettingsPanel(
                    selectedTheme = selectedTheme,
                    onThemeChange = onThemeChange,
                    centerEmoji = centerEmoji,
                    onEmojiChange = onEmojiChange,
                    spinDuration = spinDuration,
                    onDurationChange = onDurationChange,
                    isSpinning = isSpinning
                )
            }
        }
    }
}

// Names listing and Quick Action Controls Layout
@Composable
fun NamesPanel(
    names: List<String>,
    isSpinning: Boolean,
    inputName: String,
    onInputNameChange: (String) -> Unit,
    onAddName: (String) -> Unit,
    onRemoveName: (Int) -> Unit,
    onClearNames: () -> Unit,
    onShuffle: () -> Unit,
    onSort: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Quick statistics indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${names.size} entries inside the wheel",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (names.isNotEmpty() && !isSpinning) {
                TextButton(
                    onClick = onClearNames,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // TextInput row for entry additions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputName,
                onValueChange = onInputNameChange,
                placeholder = { Text("Enter a name...") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("name_input_field"),
                enabled = !isSpinning,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { onAddName(inputName) },
                enabled = !isSpinning && inputName.trim().isNotEmpty(),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .size(52.dp)
                    .testTag("add_name_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Entry")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Shuffle and Sorting Actions Grid Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onShuffle,
                enabled = !isSpinning && names.size > 1,
                modifier = Modifier
                    .weight(1f)
                    .testTag("shuffle_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Shuffle")
            }

            OutlinedButton(
                onClick = onSort,
                enabled = !isSpinning && names.size > 1,
                modifier = Modifier
                    .weight(1f)
                    .testTag("sort_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.SortByAlpha, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sort A-Z")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Names scroll list viewport
        if (names.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📭",
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Wheel is currently empty!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Add some names above to get started.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .testTag("names_lazy_list"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(10.dp)
            ) {
                itemsIndexed(names) { index, name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = name,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onRemoveName(index) },
                            enabled = !isSpinning,
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete entry",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Personalization and custom Settings Controls Panel
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsPanel(
    selectedTheme: String,
    onThemeChange: (String) -> Unit,
    centerEmoji: String,
    onEmojiChange: (String) -> Unit,
    spinDuration: Int,
    onDurationChange: (Int) -> Unit,
    isSpinning: Boolean
) {
    val themes = listOf("Rainbow", "Cosmic", "Neon", "Pastel")
    val defaultEmojis = listOf("🎯", "🍕", "🎲", "👑", "⭐", "💰", "🍀", "💡", "🦄", "🔥", "🍭", "👽")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Theme selector Section
        Column {
            Text(
                text = "Color Palette Theme",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                themes.forEach { themeName ->
                    val isSelected = themeName == selectedTheme
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = !isSpinning) { onThemeChange(themeName) }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("theme_pill_$themeName"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = themeName,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Center Emoji picker Section
        Column {
            Text(
                text = "Center Hub Logo / Emoji",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                defaultEmojis.forEach { emoji ->
                    val isSelected = emoji == centerEmoji
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = !isSpinning) { onEmojiChange(emoji) }
                            .testTag("emoji_box_$emoji"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Custom text field for user typed center symbol
            OutlinedTextField(
                value = centerEmoji,
                onValueChange = {
                    val text = it.trim()
                    if (text.length <= 4) { // Allow tiny strings or single emoji
                        onEmojiChange(text)
                    }
                },
                placeholder = { Text("Custom...") },
                label = { Text("Or Type Custom Emoji") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .testTag("custom_emoji_input"),
                enabled = !isSpinning,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }

        // Spin physics duration slider Section
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spinning Duration",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$spinDuration seconds",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = spinDuration.toFloat(),
                onValueChange = { onDurationChange(it.toInt()) },
                valueRange = 2f..15f,
                steps = 12,
                enabled = !isSpinning,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("duration_slider")
            )
        }
    }
}

// Saved wheels history BottomSheet loader
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedWheelsBottomSheet(
    wheels: List<WheelEntity>,
    onClose: () -> Unit,
    onLoadWheel: (WheelEntity) -> Unit,
    onDeleteWheel: (WheelEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        modifier = Modifier.testTag("saved_wheels_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
        ) {
            Text(
                text = "My Saved Offline Wheels",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (wheels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No saved wheels found offline. Add names and click save at the top!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(wheels.size) { index ->
                        val wheel = wheels[index]
                        OutlinedCard(
                            onClick = { onLoadWheel(wheel) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = wheel.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Theme: ${wheel.colorTheme}  •  ${wheel.namesList.size} names",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Row {
                                    Text(text = wheel.centerEmoji, fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterVertically))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { onDeleteWheel(wheel) },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Wheel")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close Menu")
            }
        }
    }
}

// Celebration Winner Alert Dialog Card with confetti triggers
@Composable
fun WinnerCelebrationDialog(
    winnerName: String,
    onClose: () -> Unit,
    onRemove: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("winner_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Celebration Visual Header
                Text(
                    text = "🎉 WINNER! 🎉",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Golden background card displaying the chosen name elegantly
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = winnerName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Core decision flow buttons: Close, or Remove Winner (Raffle Elimination)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Faithfully mimicking the absolute best feature of wheelofnames.com
                    Button(
                        onClick = onRemove,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("remove_winner_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "REMOVE WINNER FROM WHEEL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("close_winner_dialog_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "KEEP & SPIN AGAIN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// Dialog to prompt saving configuration name offline
@Composable
fun SaveWheelTitleDialog(
    initialTitle: String,
    onClose: () -> Unit,
    onSave: (String) -> Unit
) {
    var textInput by remember { mutableStateOf(initialTitle) }

    Dialog(onDismissRequest = onClose) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("save_title_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Save Wheel Config Offline",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Wheel Title Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_title_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onClose) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(textInput) },
                        enabled = textInput.trim().isNotEmpty(),
                        modifier = Modifier.testTag("save_confirm_button")
                    ) {
                        Text("Save Offline")
                    }
                }
            }
        }
    }
}

// Dialog to edit currently loaded wheel title
@Composable
fun EditWheelTitleDialog(
    initialTitle: String,
    onClose: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textInput by remember { mutableStateOf(initialTitle) }

    Dialog(onDismissRequest = onClose) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("edit_title_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Wheel Title",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_title_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onClose) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(textInput) },
                        enabled = textInput.trim().isNotEmpty(),
                        modifier = Modifier.testTag("edit_title_confirm_button")
                    ) {
                        Text("Rename")
                    }
                }
            }
        }
    }
}
