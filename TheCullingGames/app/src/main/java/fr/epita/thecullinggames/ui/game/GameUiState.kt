package fr.epita.thecullinggames.ui.game

import fr.epita.thecullinggames.business.model.CardId
import fr.epita.thecullinggames.business.model.Die
import fr.epita.thecullinggames.business.model.GameCommand
import fr.epita.thecullinggames.business.model.GameEvent
import fr.epita.thecullinggames.business.model.PendingChoice
import fr.epita.thecullinggames.business.model.Phase
import fr.epita.thecullinggames.business.model.ResolveGroup
import fr.epita.thecullinggames.business.model.TileId

/** Immutable snapshot of a player, built by the ViewModel for the View. */
data class PlayerUi(
    val id: Int,
    val characterId: String,
    val name: String,
    val isHuman: Boolean,
    val hp: Int,
    val maxHp: Int,
    val energy: Int,
    val vp: Int,
    val wickedness: Int,
    val alive: Boolean,
    val inTokyo: Boolean,
    val isActive: Boolean,
)

data class MarketCardUi(val card: CardId, val price: Int, val buyable: Boolean)

/**
 * Everything the game screen displays. [legal] contains the commands the human may send now:
 * the View enables its controls from it and never re-implements the rules.
 */
data class GameUiState(
    val players: List<PlayerUi> = emptyList(),
    val humanId: Int = 0,
    val activeId: Int = 0,
    val phase: Phase = Phase.ROLLING,
    val dice: List<Die> = emptyList(),
    val hasRolled: Boolean = false,
    val cyberDieIndex: Int? = null,
    val rollsUsed: Int = 0,
    val maxRolls: Int = 3,
    val remainingGroups: List<ResolveGroup> = emptyList(),
    val tokyoId: Int? = null,
    val market: List<MarketCardUi> = emptyList(),
    val deckTop: CardId? = null,
    val deckTopPrice: Int = 0,
    val hand: List<CardId> = emptyList(),
    val powers: List<CardId> = emptyList(),
    val tiles: List<TileId> = emptyList(),
    val wickedness: Int = 0,
    val refreshCost: Int = 2,
    val legal: List<GameCommand> = emptyList(),
    /** Decision the human must take now (reaction, tile), null otherwise. */
    val humanPending: PendingChoice? = null,
    /** Player who must decide now, null when the game is over. */
    val deciderId: Int? = null,
    /** Latest events, newest first. [logSize] is the total number of events of the game. */
    val log: List<GameEvent> = emptyList(),
    val logSize: Int = 0,
    val isOver: Boolean = false,
    val winnerId: Int? = null,
    val turnNumber: Int = 0,
) {
    val human: PlayerUi? get() = players.firstOrNull { it.id == humanId }
    fun player(id: Int) = players.first { it.id == id }
    val isHumanDecision: Boolean get() = deciderId == humanId
}

/** Arguments of the end screen. */
data class GameResult(val humanWon: Boolean, val winnerName: String?, val humanVp: Int, val humanHp: Int, val turns: Int)
