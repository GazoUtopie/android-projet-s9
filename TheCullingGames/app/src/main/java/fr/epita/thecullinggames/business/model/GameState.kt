package fr.epita.thecullinggames.business.model

/**
 * Phases a player can act in. Start of turn, Tokyo entry and end of turn are automatic
 * transitions performed by the engine, they never wait for a command.
 */
enum class Phase { ROLLING, RESOLVING, CHOOSE_TILE, BUY, GAME_OVER }

/** Full state of a game. Only the engine mutates it. */
class GameState(
    val players: List<PlayerState>,
    /** Number of participants at the start: decides the Tokyo bonuses (2 players → Energy). */
    val initialPlayerCount: Int,
) {
    var activeId = 0
    var phase = Phase.ROLLING
    var turnNumber = 0

    /** Player occupying Tokyo (the Culling Games colony), null when empty. */
    var tokyoId: Int? = null

    var dice: List<Die> = List(6) { Die(DieFace.ONE) }
    var rollsUsed = 0
    var maxRolls = 3

    /** Die counted twice by Cybercerveau this turn. */
    var cyberDieIndex: Int? = null
    val resolvedGroups = mutableSetOf<ResolveGroup>()

    /** At least one Claw face was resolved this turn (Nanami's overtime). */
    var clawsResolved = false

    /** Values (1, 2, 3) of the triples resolved this turn (Black Flash). */
    val triplesResolved = mutableSetOf<Int>()

    /** Targets that already lost an Energy to Divergent Fist this turn. */
    val divergentDrained = mutableSetOf<Int>()

    val deck = ArrayDeque<CardId>()
    val market = mutableListOf<CardId>()
    val discard = mutableListOf<CardId>()

    /** Common reserve of Wickedness tiles still available. */
    val tilePool = mutableListOf<TileId>()

    var pending: PendingChoice? = null

    var winnerId: Int? = null
    val log = mutableListOf<GameEvent>()

    fun player(id: Int) = players[id]
    val active: PlayerState get() = players[activeId]
    val human: PlayerState get() = players.first { it.isHuman }
    val alivePlayers: List<PlayerState> get() = players.filter { it.alive }
    val hasRolled: Boolean get() = rollsUsed > 0
}
