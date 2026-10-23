package fr.epita.thecullinggames.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.epita.thecullinggames.business.ai.SimpleAi
import fr.epita.thecullinggames.business.engine.GameEngine
import fr.epita.thecullinggames.business.model.CardId
import fr.epita.thecullinggames.business.model.GameCommand
import fr.epita.thecullinggames.business.model.Phase
import fr.epita.thecullinggames.business.repository.GameRepository
import fr.epita.thecullinggames.data.ServiceLocator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel of the game screen. It owns the engine for the lifetime of the screen
 * (configuration changes included), forwards the human's commands and plays the AIs
 * in a coroutine so the UI never blocks.
 */
class GameViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val repository: GameRepository = ServiceLocator.gameRepository
    private val engine: GameEngine = repository.newGame(
        humanCharacterId = savedStateHandle.get<String>("characterId") ?: "gojo",
        aiCount = savedStateHandle.get<Int>("aiCount") ?: 3,
    )
    private val ai = SimpleAi()
    private var aiJob: Job? = null

    private val _state = MutableStateFlow(snapshot())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    /** Set a short moment after the end, so the player can read the last actions. */
    private val _result = MutableStateFlow<GameResult?>(null)
    val result: StateFlow<GameResult?> = _result.asStateFlow()

    init {
        playAis()
    }

    fun send(command: GameCommand) {
        if (aiJob?.isActive == true) return
        if (engine.submit(command).accepted) {
            publish()
            playAis()
        }
    }

    /** Plays every AI decision (their turns and their reactions) until the human must decide. */
    private fun playAis() {
        if (aiJob?.isActive == true) return
        aiJob = viewModelScope.launch {
            while (true) {
                val decider = engine.decider ?: break
                if (engine.state.player(decider).isHuman) break
                val command = ai.decide(engine, decider)
                delay(paceOf(command))
                engine.submit(command)
                publish()
            }
            publish()
            if (engine.isOver && _result.value == null) {
                delay(END_DELAY_MS)
                val human = engine.state.human
                _result.value = GameResult(
                    humanWon = engine.state.winnerId == human.id,
                    winnerName = engine.state.winnerId?.let { engine.state.player(it).name },
                    humanVp = human.vp,
                    humanHp = human.hp,
                    turns = engine.state.turnNumber,
                )
            }
        }
    }

    /** Short pauses so the human can follow what the AIs do. */
    private fun paceOf(command: GameCommand): Long = when (command) {
        is GameCommand.ToggleKeep -> 120L
        is GameCommand.Roll -> 700L
        is GameCommand.EndTurn -> 600L
        else -> 450L
    }

    private fun publish() {
        _state.value = snapshot()
    }

    private fun snapshot(): GameUiState {
        val s = engine.state
        val human = s.human
        val legal = engine.legalCommands(human.id)
        return GameUiState(
            players = s.players.map {
                PlayerUi(
                    id = it.id, characterId = it.character.id, name = it.name, isHuman = it.isHuman,
                    hp = it.hp, maxHp = it.maxHp, energy = it.energy, vp = it.vp, wickedness = it.wickedness,
                    alive = it.alive, inTokyo = s.tokyoId == it.id, isActive = s.activeId == it.id,
                )
            },
            humanId = human.id,
            activeId = s.activeId,
            phase = s.phase,
            dice = s.dice,
            hasRolled = s.hasRolled,
            cyberDieIndex = s.cyberDieIndex,
            rollsUsed = s.rollsUsed,
            maxRolls = s.maxRolls,
            remainingGroups = if (s.phase == Phase.RESOLVING) engine.remainingGroups() else emptyList(),
            tokyoId = s.tokyoId,
            market = s.market.map { MarketCardUi(it, engine.priceOf(human, it), GameCommand.BuyCard(human.id, it) in legal) },
            deckTop = if (human.has(CardId.MAKI_GLASSES)) s.deck.firstOrNull() else null,
            deckTopPrice = s.deck.firstOrNull()?.let { engine.priceOf(human, it) } ?: 0,
            hand = human.hand.toList(),
            powers = human.powers.toList(),
            tiles = human.tiles.toList(),
            wickedness = human.wickedness,
            refreshCost = engine.rules.refreshCost,
            legal = legal,
            humanPending = s.pending?.takeIf { it.playerId == human.id },
            deciderId = engine.decider,
            log = s.log.takeLast(LOG_SIZE).reversed(),
            logSize = s.log.size,
            isOver = engine.isOver,
            winnerId = s.winnerId,
            turnNumber = s.turnNumber,
        )
    }

    private companion object {
        const val END_DELAY_MS = 1800L
        const val LOG_SIZE = 60
    }
}
