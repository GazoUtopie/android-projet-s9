package fr.epita.thecullinggames.business.model

/** Origin of a gain or a loss, so the View can explain what happened. */
sealed interface Source {
    data object Dice : Source
    data object Triple : Source
    data object TokyoEntry : Source
    data object TokyoStart : Source
    data object Market : Source
    data class Card(val id: CardId) : Source
    data class Tile(val id: TileId) : Source
}

/** Facts produced by the engine. The View turns them into a readable log. */
sealed interface GameEvent {
    data class FirstPlayer(val playerId: Int) : GameEvent
    data class TurnStarted(val playerId: Int) : GameEvent
    data class Rolled(val playerId: Int, val rollNumber: Int, val faces: List<DieFace>) : GameEvent
    data class DieChanged(val playerId: Int, val source: Source, val face: DieFace) : GameEvent
    data class VpChanged(val playerId: Int, val delta: Int, val source: Source) : GameEvent
    data class EnergyChanged(val playerId: Int, val delta: Int, val source: Source) : GameEvent
    data class HpChanged(val playerId: Int, val delta: Int, val source: Source) : GameEvent
    data class WickednessGained(val playerId: Int, val amount: Int) : GameEvent
    data class Damaged(val attackerId: Int, val targetId: Int, val amount: Int, val source: Source) : GameEvent
    data class DamageCancelled(val targetId: Int, val source: Source) : GameEvent
    data class DamageReduced(val targetId: Int, val amount: Int, val source: Source) : GameEvent
    data class EnteredTokyo(val playerId: Int) : GameEvent
    data class LeftTokyo(val playerId: Int) : GameEvent
    data class TileAcquired(val playerId: Int, val tile: TileId) : GameEvent
    data class CardBought(val playerId: Int, val card: CardId, val price: Int) : GameEvent
    data class MarketRefreshed(val playerId: Int) : GameEvent
    data class PowerActivated(val playerId: Int, val card: CardId) : GameEvent
    data class ActionPlayed(val playerId: Int, val card: CardId) : GameEvent
    data class Eliminated(val playerId: Int) : GameEvent
    data class GameOver(val winnerId: Int?, val humanWon: Boolean) : GameEvent
}
