package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.WheelEntity
import com.example.data.WheelRepository
import com.example.sound.SoundSynthesizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WheelViewModel(
    private val repository: WheelRepository,
    val soundSynthesizer: SoundSynthesizer
) : ViewModel() {

    // All offline saved wheels from database
    val savedWheels: StateFlow<List<WheelEntity>> = repository.allWheels
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current Wheel state under edit/play
    private val _currentWheelId = MutableStateFlow<Int?>(null)
    val currentWheelId = _currentWheelId.asStateFlow()

    private val _wheelTitle = MutableStateFlow("Dinner Decisions")
    val wheelTitle = _wheelTitle.asStateFlow()

    private val _namesList = MutableStateFlow(
        listOf("Pizza 🍕", "Burgers 🍔", "Sushi 🍣", "Salad 🥗", "Tacos 🌮", "Thai Food 🍜", "Pasta 🍝", "Steak 🥩")
    )
    val namesList = _namesList.asStateFlow()

    private val _colorTheme = MutableStateFlow("Rainbow")
    val colorTheme = _colorTheme.asStateFlow()

    private val _centerEmoji = MutableStateFlow("🎯")
    val centerEmoji = _centerEmoji.asStateFlow()

    private val _spinDuration = MutableStateFlow(5) // Default 5 seconds
    val spinDuration = _spinDuration.asStateFlow()

    // Interactive states
    private val _isSpinning = MutableStateFlow(false)
    val isSpinning = _isSpinning.asStateFlow()

    private val _winner = MutableStateFlow<String?>(null)
    val winner = _winner.asStateFlow()

    private val _showWinnerDialog = MutableStateFlow(false)
    val showWinnerDialog = _showWinnerDialog.asStateFlow()

    private val _inputName = MutableStateFlow("")
    val inputName = _inputName.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled = _soundEnabled.asStateFlow()

    private val _rotationAngle = MutableStateFlow(0f)
    val rotationAngle = _rotationAngle.asStateFlow()

    init {
        soundSynthesizer.isSoundEnabled = _soundEnabled.value
    }

    fun setInputName(text: String) {
        _inputName.value = text
    }

    fun toggleSound() {
        _soundEnabled.value = !_soundEnabled.value
        soundSynthesizer.isSoundEnabled = _soundEnabled.value
    }

    fun addName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            _namesList.value = _namesList.value + trimmed
            _inputName.value = ""
        }
    }

    fun removeName(index: Int) {
        if (index in _namesList.value.indices) {
            val updated = _namesList.value.toMutableList()
            updated.removeAt(index)
            _namesList.value = updated
        }
    }

    fun clearNames() {
        _namesList.value = emptyList()
    }

    fun shuffleNames() {
        _namesList.value = _namesList.value.shuffled()
    }

    fun sortNames() {
        _namesList.value = _namesList.value.sorted()
    }

    fun updateTheme(theme: String) {
        _colorTheme.value = theme
    }

    fun updateCenterEmoji(emoji: String) {
        _centerEmoji.value = emoji
    }

    fun updateSpinDuration(seconds: Int) {
        _spinDuration.value = seconds.coerceIn(2, 15)
    }

    fun updateWheelTitle(title: String) {
        _wheelTitle.value = title.trim()
    }

    // Spin management
    fun startSpinning() {
        if (_isSpinning.value || _namesList.value.isEmpty()) return
        _isSpinning.value = true
        _winner.value = null
        _showWinnerDialog.value = false
    }

    fun finishSpinning(winningIndex: Int) {
        _isSpinning.value = false
        if (winningIndex in _namesList.value.indices) {
            _winner.value = _namesList.value[winningIndex]
            _showWinnerDialog.value = true
            soundSynthesizer.playWin()
        }
    }

    fun closeWinnerDialog() {
        _showWinnerDialog.value = false
    }

    // Faithfully matches wheelofnames.com remove-winner action
    fun removeWinnerAndClose() {
        val winnerName = _winner.value
        if (winnerName != null) {
            val updated = _namesList.value.toMutableList()
            updated.remove(winnerName)
            _namesList.value = updated
        }
        _winner.value = null
        _showWinnerDialog.value = false
    }

    fun updateRotationAngle(angle: Float) {
        _rotationAngle.value = angle
    }

    // Load saved configurations
    fun loadWheel(wheel: WheelEntity) {
        _currentWheelId.value = wheel.id
        _wheelTitle.value = wheel.title
        _namesList.value = wheel.namesList
        _colorTheme.value = wheel.colorTheme
        _centerEmoji.value = wheel.centerEmoji
        _spinDuration.value = wheel.spinDuration
        _winner.value = null
        _showWinnerDialog.value = false
    }

    fun startNewWheel() {
        _currentWheelId.value = null
        _wheelTitle.value = "New Decision"
        _namesList.value = listOf("Yes", "No", "Maybe", "Spin Again")
        _colorTheme.value = "Rainbow"
        _centerEmoji.value = "🎯"
        _spinDuration.value = 5
        _winner.value = null
        _showWinnerDialog.value = false
    }

    // Database Actions
    fun saveWheel(customTitle: String? = null) {
        val titleToSave = (customTitle ?: _wheelTitle.value).trim().ifEmpty { "My Wheel" }
        _wheelTitle.value = titleToSave
        
        val namesStr = _namesList.value.joinToString("\n")
        val wheel = WheelEntity(
            id = _currentWheelId.value ?: 0,
            title = titleToSave,
            namesString = namesStr,
            colorTheme = _colorTheme.value,
            centerEmoji = _centerEmoji.value,
            spinDuration = _spinDuration.value,
            lastModified = System.currentTimeMillis()
        )

        viewModelScope.launch {
            val insertedId = repository.insertWheel(wheel)
            if (_currentWheelId.value == null) {
                _currentWheelId.value = insertedId.toInt()
            }
        }
    }

    fun deleteWheel(wheel: WheelEntity) {
        viewModelScope.launch {
            repository.deleteWheel(wheel)
            if (_currentWheelId.value == wheel.id) {
                startNewWheel()
            }
        }
    }
}

class WheelViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = AppDatabase.getDatabase(context)
        val repo = WheelRepository(db.wheelDao())
        val synth = SoundSynthesizer(context)
        return WheelViewModel(repo, synth) as T
    }
}
