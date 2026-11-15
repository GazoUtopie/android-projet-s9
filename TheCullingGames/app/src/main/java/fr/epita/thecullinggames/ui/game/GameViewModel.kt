package fr.epita.thecullinggames.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.epita.thecullinggames.business.ai.SimpleAi
import fr.epita.thecullinggames.business.engine.GameEngine
import fr.epita.thecullinggames.business.model.CardId
import fr.epita.thecullinggames.business.model.GameCommand
import fr.epita.thecullinggames.business.model.GameEvent
import fr.epita.thecullinggames.business.model.TileId
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

    /**
     * Feedback being shown now. Items wait in a queue so they never overlap, and the game never
     * waits for them. The queue lives here: it survives a rotation and nothing is replayed.
     */
    private val _feedback = MutableStateFlow<Feedback?>(null)
    val feedback: StateFlow<Feedback?> = _feedback.asStateFlow()
    private val feedbackQueue = ArrayDeque<Feedback>()
    private var feedbackJob: Job? = null
    private var nextFeedbackId = 0L

    /** Id of the last item whose animation the View already started (not restarted after a rotation). */
    var displayedFeedbackId: Long = -1L
        private set

    init {
        playAis()
    }

    fun send(command: GameCommand) {
        if (aiJob?.isActive == true) return
        val result = engine.submit(command)
        if (result.accepted) {
            enqueueFeedback(result.events)
            publish()
            playAis()
        }
    }

    fun markFeedbackDisplayed(id: Long) {
        displayedFeedbackId = id
    }

    private fun enqueueFeedback(events: List<GameEvent>) {
        feedbackQueue += FeedbackBuilder.build(events) { nextFeedbackId++ }
        if (feedbackJob?.isActive == true || feedbackQueue.isEmpty()) return
        feedbackJob = viewModelScope.launch {
            while (feedbackQueue.isNotEmpty()) {
                val item = feedbackQueue.removeFirst()
                _feedback.value = item
                delay(durationOf(item, backlog = feedbackQueue.size))
            }
            _feedback.value = null
        }
    }

    /** Shorter display when items pile up during the AI turns, so the feedback keeps up with the game. */
    private fun durationOf(item: Feedback, backlog: Int): Long {
        val base = when (item) {
            is Feedback.Showcase -> SHOWCASE_MS
            is Feedback.Impacts -> IMPACTS_MS
            else -> BANNER_MS
        }
        return if (backlog >= 2) base * 3 / 5 else base
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
                enqueueFeedback(engine.submit(command).events)
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
                    tiles = it.tiles.toList(), powers = it.powers.toList(),
                    effects = if (!it.alive) emptyList() else buildList {
                        // Direct reads of the engine counters, no rule evaluated here.
                        if (it.divergentFistNextTurn) add(PlayerEffect.DIVERGENT_FIST_NEXT_TURN)
                        if (it.divergentFistActive && s.activeId == it.id) add(PlayerEffect.DIVERGENT_FIST_ACTIVE)
                        if (it.infinityUsed) add(PlayerEffect.INFINITY_USED)
                        if (it.specialGradeUsed) add(PlayerEffect.SPECIAL_GRADE_USED)
                        if (it.has(TileId.CYBERCERVEAU) && !it.cyberBrainReady) add(PlayerEffect.CYBER_BRAIN_NEXT_TURN)
                    },
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
            market = s.market.map {
                MarketCardUi(it, engine.priceOf(human, it), GameCommand.BuyCard(human.id, it) in legal, engine.purchaseBlock(human.id, it))
            },
            deckTop = if (human.has(CardId.MAKI_GLASSES)) s.deck.firstOrNull() else null,
            deckTopPrice = s.deck.firstOrNull()?.let { engine.priceOf(human, it) } ?: 0,
            deckTopBlock = s.deck.firstOrNull()?.let { engine.purchaseBlock(human.id, it) },
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
        const val BANNER_MS = 1600L
        const val SHOWCASE_MS = 2200L
        const val IMPACTS_MS = 1100L
        const val LOG_SIZE = 60
    }
}
