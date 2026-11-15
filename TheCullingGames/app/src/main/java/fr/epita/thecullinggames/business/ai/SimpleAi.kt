package fr.epita.thecullinggames.business.ai

import fr.epita.thecullinggames.business.engine.GameEngine
import fr.epita.thecullinggames.business.model.CardId
import fr.epita.thecullinggames.business.model.DieFace
import fr.epita.thecullinggames.business.model.GameCommand
import fr.epita.thecullinggames.business.model.PendingChoice
import fr.epita.thecullinggames.business.model.Phase
import fr.epita.thecullinggames.business.model.PlayerState
import fr.epita.thecullinggames.business.model.ResolveGroup
import fr.epita.thecullinggames.business.model.TileId

/**
 * Simple rule-based AI. It never builds commands by itself: it always picks one of
 * [GameEngine.legalCommands], so it plays with exactly the same rights as the human.
 */
class SimpleAi {

    /** Turn number in which this AI already renewed the market (avoids endless refreshes). */
    private val refreshedOnTurn = mutableMapOf<Int, Int>()
    private val talismanOnTurn = mutableMapOf<Int, Int>()

    fun decide(engine: GameEngine, playerId: Int): GameCommand {
        val legal = engine.legalCommands(playerId)
        check(legal.isNotEmpty()) { "No decision expected from player $playerId" }
        val me = engine.state.player(playerId)
        val choice = when (val pending = engine.state.pending) {
            is PendingChoice.Infinity -> GameCommand.AnswerInfinity(playerId, pending.damage >= 2 || pending.damage >= me.hp)
            is PendingChoice.Flee -> GameCommand.AnswerFlee(playerId, me.hp <= FLEE_HP)
            is PendingChoice.ChooseTile -> GameCommand.ChooseTile(playerId, pending.options.minBy { TILE_PRIORITY.indexOf(it) })
            is PendingChoice.CardTargets -> chooseTarget(engine, me, pending)
            null -> when (engine.state.phase) {
                Phase.ROLLING -> rolling(engine, me, legal)
                Phase.RESOLVING -> resolving(engine, me, legal)
                Phase.BUY -> buying(engine, me, legal)
                else -> null
            }
        }
        return choice?.takeIf { it in legal } ?: legal.first()
    }

    private fun rolling(engine: GameEngine, me: PlayerState, legal: List<GameCommand>): GameCommand {
        val state = engine.state
        if (!state.hasRolled) return GameCommand.Roll(me.id)
        val wanted = wantedDice(engine, me)
        state.dice.forEachIndexed { index, die ->
            val toggle = GameCommand.ToggleKeep(me.id, index)
            if (die.kept != (index in wanted) && toggle in legal) return toggle
        }
        val roll = GameCommand.Roll(me.id)
        if (roll in legal) return roll
        val incantation = GameCommand.UseIncantation(me.id)
        if (incantation in legal && me.energy >= 3 && state.dice.indices.any { it !in wanted }) return incantation
        return GameCommand.StopRolling(me.id)
    }

    /** Indices of the dice worth keeping, given the situation of the AI. */
    private fun wantedDice(engine: GameEngine, me: PlayerState): Set<Int> {
        val state = engine.state
        val inTokyo = state.tokyoId == me.id
        val attacking = inTokyo || state.tokyoId == null || me.hp > FLEE_HP
        val counts = state.dice.groupingBy { it.face }.eachCount()
        return state.dice.indices.filter { index ->
            when (val face = state.dice[index].face) {
                DieFace.CLAW -> attacking
                DieFace.ENERGY -> true
                DieFace.HEART -> !inTokyo && !me.has(CardId.BINDING_VOW) && me.hp <= me.maxHp - 2
                else -> counts.getValue(face) >= 2
            }
        }.toSet()
    }

    private fun resolving(engine: GameEngine, me: PlayerState, legal: List<GameCommand>): GameCommand {
        val state = engine.state
        val counts = state.dice.groupingBy { it.face }.eachCount()
        // 10 Shadows: turn a lonely 1 or 2 into a Claw.
        legal.filterIsInstance<GameCommand.UseTenShadows>()
            .firstOrNull { counts.getValue(state.dice[it.dieIndex].face) < 3 }
            ?.let { return it }
        // Cybercerveau: complete a triple, otherwise strengthen the attack.
        val cyber = legal.filterIsInstance<GameCommand.UseCyberBrain>()
        if (cyber.isNotEmpty()) {
            val pair = cyber.firstOrNull { state.dice[it.dieIndex].face.number != null && counts[state.dice[it.dieIndex].face] == 2 }
            val claw = cyber.firstOrNull { state.dice[it.dieIndex].face == DieFace.CLAW }
            (pair ?: claw)?.let { return it }
        }
        // Claws last, so that gains are secured before a possible fight.
        return listOf(ResolveGroup.NUMBERS, ResolveGroup.ENERGY, ResolveGroup.HEARTS, ResolveGroup.CLAWS)
            .map { GameCommand.Resolve(me.id, it) }
            .first { it in legal }
    }

    private fun buying(engine: GameEngine, me: PlayerState, legal: List<GameCommand>): GameCommand {
        val state = engine.state
        // Cards are used at purchase: only buy what is useful right now.
        val purchase = legal.filterIsInstance<GameCommand.BuyCard>()
            .filter { worthBuying(engine, me, it.card) }
            .maxByOrNull { engine.cardDef(it.card).cost }
        if (purchase != null) {
            if (purchase.card == CardId.TALISMAN) talismanOnTurn[me.id] = state.turnNumber
            return purchase
        }

        val refresh = GameCommand.RefreshMarket(me.id)
        if (refresh in legal && me.energy >= 7 && refreshedOnTurn[me.id] != state.turnNumber) {
            refreshedOnTurn[me.id] = state.turnNumber
            return refresh
        }
        return GameCommand.EndTurn(me.id)
    }

    private fun worthBuying(engine: GameEngine, me: PlayerState, card: CardId): Boolean {
        val state = engine.state
        return when (card) {
            CardId.REVERSE_CURSED_TECHNIQUE -> me.hp <= me.maxHp - 4 || me.hp <= 4
            CardId.OVERFLOWING_ENERGY -> me.hp <= me.maxHp - 3 || me.hp <= 4
            CardId.BLOOD_MANIPULATION -> me.hp >= 6
            CardId.RESONANCE -> state.alivePlayers.any { it.id != me.id && it.energy >= 2 }
            // Outside: take Tokyo when healthy. Inside: escape when hurt.
            CardId.BOOGIE_WOOGIE -> if (state.tokyoId == me.id) me.hp <= FLEE_HP else me.hp >= 7
            // Talisman can pay for itself with Repaire maléfique: once per turn at most.
            CardId.TALISMAN -> talismanOnTurn[me.id] != state.turnNumber
            CardId.SUKUNA_FINGER -> me.hp > 6
            in UNWANTED_POWERS -> false
            else -> true
        }
    }

    /** Target of an Action just bought: the weakest opponent, the richest for Resonance. */
    private fun chooseTarget(engine: GameEngine, me: PlayerState, pending: PendingChoice.CardTargets): GameCommand {
        val state = engine.state
        val weakest = state.alivePlayers.filter { it.id != me.id }.minBy { it.hp }.id
        val option = when (pending.card) {
            CardId.RESONANCE -> pending.options.maxBy { state.player(it.target!!).energy }
            // Violet: everything on the weakest opponent (single target, or the whole split from Tokyo).
            CardId.VIOLET -> pending.options.maxBy { it.allocation[weakest] ?: if (it.target == weakest) 1 else 0 }
            else -> pending.options.minBy { state.player(it.target!!).hp }
        }
        return GameCommand.ChooseCardTarget(me.id, option)
    }

    private companion object {
        const val FLEE_HP = 5

        val TILE_PRIORITY = listOf(
            TileId.RAYON_ANTIMATIERE, TileId.RAYON_STELLAIRE,
            TileId.REGENERATION_TOTALE, TileId.PANIQUE_GENERALE, TileId.CYBERCERVEAU, TileId.REPAIRE_MALEFIQUE,
            TileId.ETERNEL, TileId.SOURNOIS, TileId.INFATIGABLE, TileId.FURTIF,
        )

        /** Powers the simple AI does not know how to exploit. */
        val UNWANTED_POWERS = setOf(CardId.MAKI_GLASSES)
    }
}
