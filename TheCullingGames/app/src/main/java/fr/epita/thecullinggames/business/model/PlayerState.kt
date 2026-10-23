package fr.epita.thecullinggames.business.model

/**
 * State of one monster. Only [fr.epita.thecullinggames.business.engine.GameEngine] mutates it;
 * the View reads immutable copies built by the ViewModel.
 */
class PlayerState(
    val id: Int,
    val character: Character,
    val isHuman: Boolean,
    var hp: Int,
    var maxHp: Int,
) {
    var energy = 0
    var vp = 0
    var wickedness = 0
    var alive = true

    /** Bought cards not used yet (Actions and inactive Powers). */
    val hand = mutableListOf<CardId>()

    /** Activated Powers: permanent until elimination. */
    val powers = mutableSetOf<CardId>()

    /** Acquired Wickedness tiles (Panique générale is discarded right after its effect). */
    val tiles = mutableSetOf<TileId>()

    /** Wickedness levels (3, 6, 10) already used: one tile per level and per player. */
    val usedTileLevels = mutableSetOf<Int>()

    // Counters reset at the start of the owner's turn (provisional convention, A_CLARIFIER.md).
    var tenShadowsUsed = false
    var incantationUsed = false
    var cyberBrainUsed = false
    var infinityUsed = false
    var specialGradeUsed = false

    /** Cybercerveau cannot modify dice already resolved: usable from the next own turn. */
    var cyberBrainReady = false

    /** Divergent Fist played this turn, active during the next own turn. */
    var divergentFistNextTurn = false
    var divergentFistActive = false

    fun has(card: CardId) = card in powers
    fun has(tile: TileId) = tile in tiles

    val name: String get() = character.name
}
