package fr.epita.thecullinggames.ui.select

import androidx.lifecycle.ViewModel
import fr.epita.thecullinggames.business.model.Character
import fr.epita.thecullinggames.data.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SelectUiState(val characters: List<Character>, val selectedId: String?, val aiCount: Int)

/** Keeps the selection across configuration changes. */
class CharacterSelectViewModel : ViewModel() {

    private val _state = MutableStateFlow(
        SelectUiState(ServiceLocator.characterRepository.all(), selectedId = null, aiCount = 3),
    )
    val state: StateFlow<SelectUiState> = _state.asStateFlow()

    fun select(id: String) {
        _state.value = _state.value.copy(selectedId = id)
    }

    fun setAiCount(count: Int) {
        _state.value = _state.value.copy(aiCount = count)
    }
}
