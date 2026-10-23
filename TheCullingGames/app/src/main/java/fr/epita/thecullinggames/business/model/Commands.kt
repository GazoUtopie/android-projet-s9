package fr.epita.thecullinggames.business.model

/**
 * Commands sent by the human (through the ViewModel) or by an AI.
 * Both use exactly the same commands: the engine only accepts those listed by `legalCommands`.
 */
sealed interface GameCommand {
    val playerId: Int

    data class Roll(override val playerId: Int) : GameCommand
    data class ToggleKeep(override val playerId: Int, val dieIndex: Int) : GameCommand
    data class StopRolling(override val playerId: Int) : GameCommand
    data class UseIncantation(override val playerId: Int) : GameCommand
    data class UseTenShadows(override val playerId: Int, val dieIndex: Int) : GameCommand
    data class UseCyberBrain(override val playerId: Int, val dieIndex: Int) : GameCommand
    data class Resolve(override val playerId: Int, val group: ResolveGroup) : GameCommand
    data class ChooseTile(override val playerId: Int, val tile: TileId) : GameCommand
    data class BuyCard(override val playerId: Int, val card: CardId) : GameCommand
    data class BuyTopOfDeck(override val playerId: Int) : GameCommand
    data class RefreshMarket(override val playerId: Int) : GameCommand
    data class ActivatePower(override val playerId: Int, val card: CardId) : GameCommand

    /**
     * Plays an Action card from the hand.
     * [target] is used by single-target cards, [allocation] (player id → damage) by Violet from Tokyo.
     */
    data class PlayAction(
        override val playerId: Int,
        val card: CardId,
        val target: Int? = null,
        val allocation: Map<Int, Int> = emptyMap(),
    ) : GameCommand

    data class AnswerInfinity(override val playerId: Int, val use: Boolean) : GameCommand
    data class AnswerFlee(override val playerId: Int, val flee: Boolean) : GameCommand
    data class EndTurn(override val playerId: Int) : GameCommand
}

/** A decision the engine waits for. It may belong to a player other than the active one. */
sealed interface PendingChoice {
    val playerId: Int

    /** The owner of Infinity may pay to cancel an attack received. */
    data class Infinity(override val playerId: Int, val attackerId: Int, val damage: Int) : PendingChoice

    /** The occupant of Tokyo was wounded by Claws and may leave. */
    data class Flee(override val playerId: Int, val attackerId: Int) : PendingChoice

    /** A Wickedness threshold was reached: choose one available tile of this level. */
    data class ChooseTile(override val playerId: Int, val level: Int, val options: List<TileId>) : PendingChoice
}
