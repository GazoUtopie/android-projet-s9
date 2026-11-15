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

    /** Target chosen for the Action just bought (answer to [PendingChoice.CardTargets]). */
    data class ChooseCardTarget(override val playerId: Int, val option: TargetOption) : GameCommand

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

    /** An Action was just bought and needs a target or a damage split before it resolves. */
    data class CardTargets(override val playerId: Int, val card: CardId, val options: List<TargetOption>) : PendingChoice
}

/**
 * Target of an Action: [target] for single-target cards, [allocation] (player id → damage) for Violet from Tokyo.
 * The default value means "no choice needed".
 */
data class TargetOption(val target: Int? = null, val allocation: Map<Int, Int> = emptyMap())

/** Why a card cannot be bought now (shown on the card sheet). */
enum class PurchaseBlock { NOT_BUY_PHASE, NOT_ENOUGH_ENERGY, NO_TRIPLE, TOKYO_EMPTY, ONE_HP, NO_TARGET }
