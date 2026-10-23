package fr.epita.thecullinggames.business.engine

import fr.epita.thecullinggames.business.model.CardDef
import fr.epita.thecullinggames.business.model.CardId
import fr.epita.thecullinggames.business.model.CardType
import fr.epita.thecullinggames.business.model.Character
import fr.epita.thecullinggames.business.model.Die
import fr.epita.thecullinggames.business.model.DieFace
import fr.epita.thecullinggames.business.model.GameCommand
import fr.epita.thecullinggames.business.model.GameEvent
import fr.epita.thecullinggames.business.model.GameState
import fr.epita.thecullinggames.business.model.PendingChoice
import fr.epita.thecullinggames.business.model.Phase
import fr.epita.thecullinggames.business.model.PlayerState
import fr.epita.thecullinggames.business.model.ResolveGroup
import fr.epita.thecullinggames.business.model.Source
import fr.epita.thecullinggames.business.model.TileId
import fr.epita.thecullinggames.business.model.group

/** Result of [GameEngine.submit]: the events produced, or the reason of a refusal. */
data class SubmitResult(val accepted: Boolean, val events: List<GameEvent>, val error: String? = null)

/**
 * Rules engine of The Culling Games (King of Tokyo variant). Pure Kotlin, independent of Android.
 *
 * Flow: the engine lists the legal commands of the player expected to decide ([decider]);
 * [submit] validates a command against that list, applies it and returns the events produced.
 * The human (through the ViewModel) and the AIs use exactly the same API.
 */
class GameEngine(
    characters: List<Character>,
    cards: List<CardDef>,
    tiles: List<TileId>,
    private val random: RandomSource,
    val rules: RulesConfig = RulesConfig(),
) {
    private val cardDefs: Map<CardId, CardDef> = cards.associateBy { it.id }

    val state = GameState(
        players = characters.mapIndexed { index, character ->
            PlayerState(index, character, isHuman = index == 0, hp = rules.startHp, maxHp = rules.baseMaxHp)
        },
        initialPlayerCount = characters.size,
    )

    private val events = mutableListOf<GameEvent>()

    /** Damage waiting to be applied, possibly paused by an Infinity decision. */
    private val damageQueue = ArrayDeque<DamagePacket>()
    private var afterDamage: AfterDamage = AfterDamage.NOTHING
    private var tokyoWoundedByClaws = false

    private class DamagePacket(
        val attackerId: Int,
        val targetId: Int,
        val amount: Int,
        val source: Source,
        val fromClaws: Boolean,
        val bypassDefenses: Boolean,
    ) {
        var infinityAnswer: Boolean? = null
    }

    private enum class AfterDamage { NOTHING, CLAWS }

    init {
        require(characters.size in 2..4) { "2 to 4 participants" }
        val deck = cards.map { it.id }.toMutableList()
        random.shuffle(deck)
        state.deck.addAll(deck)
        state.tilePool.addAll(tiles)
        refillMarket()
        state.activeId = chooseFirstPlayer()
        emit(GameEvent.FirstPlayer(state.activeId))
        beginTurn()
        state.log.addAll(events)
        events.clear()
    }

    fun cardDef(id: CardId): CardDef = cardDefs.getValue(id)

    /** Price paid by [player] for [card], after Repaire maléfique. */
    fun priceOf(player: PlayerState, card: CardId): Int {
        val discount = if (player.has(TileId.REPAIRE_MALEFIQUE)) rules.maleficLairDiscount else 0
        return (cardDef(card).cost - discount).coerceAtLeast(0)
    }

    /** Player whose decision is expected now, or null when the game is over. */
    val decider: Int?
        get() = when {
            state.phase == Phase.GAME_OVER -> null
            state.pending != null -> state.pending!!.playerId
            else -> state.activeId
        }

    val isOver: Boolean get() = state.phase == Phase.GAME_OVER

    // ---------------------------------------------------------------------------------------------
    // Legal commands
    // ---------------------------------------------------------------------------------------------

    fun legalCommands(playerId: Int): List<GameCommand> {
        if (isOver || playerId != decider) return emptyList()
        when (val pending = state.pending) {
            is PendingChoice.Infinity -> return listOf(
                GameCommand.AnswerInfinity(playerId, true),
                GameCommand.AnswerInfinity(playerId, false),
            )
            is PendingChoice.Flee -> return listOf(
                GameCommand.AnswerFlee(playerId, true),
                GameCommand.AnswerFlee(playerId, false),
            )
            is PendingChoice.ChooseTile -> return pending.options.map { GameCommand.ChooseTile(playerId, it) }
            null -> Unit
        }

        val player = state.player(playerId)
        val commands = mutableListOf<GameCommand>()
        when (state.phase) {
            Phase.ROLLING -> {
                if (state.rollsUsed < state.maxRolls && (!state.hasRolled || state.dice.any { !it.kept })) {
                    commands += GameCommand.Roll(playerId)
                }
                if (state.hasRolled) {
                    if (state.rollsUsed < state.maxRolls) {
                        state.dice.indices.forEach { commands += GameCommand.ToggleKeep(playerId, it) }
                    }
                    commands += GameCommand.StopRolling(playerId)
                    if (player.has(CardId.INCANTATION) && !player.incantationUsed && player.energy >= 1 &&
                        state.maxRolls < rules.maxRollsCap
                    ) {
                        commands += GameCommand.UseIncantation(playerId)
                    }
                    commands += diceModifiers(player)
                }
                commands += cardUses(player)
            }
            Phase.RESOLVING -> {
                if (state.resolvedGroups.isEmpty()) commands += diceModifiers(player)
                remainingGroups().forEach { commands += GameCommand.Resolve(playerId, it) }
            }
            Phase.BUY -> {
                state.market.filter { priceOf(player, it) <= player.energy }
                    .forEach { commands += GameCommand.BuyCard(playerId, it) }
                if (player.has(CardId.MAKI_GLASSES)) {
                    val top = peekDeck()
                    if (top != null && priceOf(player, top) <= player.energy) {
                        commands += GameCommand.BuyTopOfDeck(playerId)
                    }
                }
                if (player.energy >= rules.refreshCost) commands += GameCommand.RefreshMarket(playerId)
                commands += cardUses(player)
                commands += GameCommand.EndTurn(playerId)
            }
            Phase.CHOOSE_TILE, Phase.GAME_OVER -> Unit
        }
        return commands
    }

    /** 10 Shadows and Cybercerveau: allowed after a roll, until the first group is resolved. */
    private fun diceModifiers(player: PlayerState): List<GameCommand> {
        val commands = mutableListOf<GameCommand>()
        if (player.has(CardId.TEN_SHADOWS) && !player.tenShadowsUsed) {
            state.dice.forEachIndexed { index, die ->
                if (die.face == DieFace.ONE || die.face == DieFace.TWO) {
                    commands += GameCommand.UseTenShadows(player.id, index)
                }
            }
        }
        if (player.has(TileId.CYBERCERVEAU) && player.cyberBrainReady && !player.cyberBrainUsed) {
            state.dice.indices.forEach { commands += GameCommand.UseCyberBrain(player.id, it) }
        }
        return commands
    }

    /** Powers to activate and Actions to play from the hand, during the owner's own turn. */
    private fun cardUses(player: PlayerState): List<GameCommand> {
        val commands = mutableListOf<GameCommand>()
        val opponents = state.alivePlayers.filter { it.id != player.id }
        val inTokyo = state.tokyoId == player.id
        for (card in player.hand.distinct()) {
            if (cardDef(card).type == CardType.POWER) {
                commands += GameCommand.ActivatePower(player.id, card)
                continue
            }
            when (card) {
                CardId.REVERSE_CURSED_TECHNIQUE, CardId.OVERFLOWING_ENERGY, CardId.DIVERGENT_FIST,
                CardId.TALISMAN -> commands += GameCommand.PlayAction(player.id, card)

                CardId.BLACK_FLASH -> if (state.phase == Phase.BUY && state.triplesResolved.isNotEmpty()) {
                    commands += GameCommand.PlayAction(player.id, card)
                }

                CardId.VIOLET -> if (inTokyo) {
                    allocations(opponents.map { it.id }, rules.violetTokyoTotal).forEach {
                        commands += GameCommand.PlayAction(player.id, card, allocation = it)
                    }
                } else {
                    opponents.forEach { commands += GameCommand.PlayAction(player.id, card, target = it.id) }
                }

                CardId.RESONANCE -> opponents.forEach {
                    commands += GameCommand.PlayAction(player.id, card, target = it.id)
                }

                // Swap with the occupant of Tokyo "et inversement": from Tokyo, swap with a player outside.
                CardId.BOOGIE_WOOGIE -> when (state.tokyoId) {
                    null -> Unit
                    player.id -> opponents.forEach { commands += GameCommand.PlayAction(player.id, card, target = it.id) }
                    else -> commands += GameCommand.PlayAction(player.id, card, target = state.tokyoId)
                }

                CardId.BLOOD_MANIPULATION -> if (player.hp > rules.bloodManipulationCost) {
                    opponents.filter { it.id != state.tokyoId }.forEach {
                        commands += GameCommand.PlayAction(player.id, card, target = it.id)
                    }
                }

                else -> Unit
            }
        }
        return commands
    }

    /** All the ways to split [total] damage between [targets] (zero allowed for a target). */
    private fun allocations(targets: List<Int>, total: Int): List<Map<Int, Int>> {
        if (targets.isEmpty()) return emptyList()
        if (targets.size == 1) return listOf(mapOf(targets[0] to total))
        val result = mutableListOf<Map<Int, Int>>()
        for (first in total downTo 0) {
            allocations(targets.drop(1), total - first).forEach { rest ->
                result += (mapOf(targets[0] to first) + rest).filterValues { it > 0 }
            }
        }
        return result.distinct()
    }

    // ---------------------------------------------------------------------------------------------
    // Submit
    // ---------------------------------------------------------------------------------------------

    fun submit(command: GameCommand): SubmitResult {
        if (command !in legalCommands(command.playerId)) {
            return SubmitResult(false, emptyList(), "Commande illégale : $command")
        }
        events.clear()
        apply(command)
        // The discard pile is reshuffled as soon as the deck is empty, and the market kept at three cards.
        if (state.deck.isEmpty()) reshuffleDiscard()
        refillMarket()
        val produced = events.toList()
        state.log.addAll(produced)
        events.clear()
        return SubmitResult(true, produced)
    }

    private fun apply(command: GameCommand) {
        val player = state.player(command.playerId)
        when (command) {
            is GameCommand.Roll -> roll(player)
            is GameCommand.ToggleKeep -> state.dice = state.dice.mapIndexed { i, die ->
                if (i == command.dieIndex) die.copy(kept = !die.kept) else die
            }
            is GameCommand.StopRolling -> state.phase = Phase.RESOLVING
            is GameCommand.UseIncantation -> {
                player.energy -= 1
                player.incantationUsed = true
                state.maxRolls += 1
                emit(GameEvent.EnergyChanged(player.id, -1, Source.Card(CardId.INCANTATION)))
            }
            is GameCommand.UseTenShadows -> {
                player.tenShadowsUsed = true
                setFace(command.dieIndex, DieFace.CLAW)
                emit(GameEvent.DieChanged(player.id, Source.Card(CardId.TEN_SHADOWS), DieFace.CLAW))
            }
            is GameCommand.UseCyberBrain -> {
                player.cyberBrainUsed = true
                state.cyberDieIndex = command.dieIndex
                emit(GameEvent.DieChanged(player.id, Source.Tile(TileId.CYBERCERVEAU), state.dice[command.dieIndex].face))
            }
            is GameCommand.Resolve -> resolve(player, command.group)
            is GameCommand.ChooseTile -> {
                state.pending = null
                acquireTile(player, command.tile)
                continueAfterResolution()
            }
            is GameCommand.BuyCard -> buy(player, command.card, fromMarket = true)
            is GameCommand.BuyTopOfDeck -> buy(player, peekDeck()!!, fromMarket = false)
            is GameCommand.RefreshMarket -> {
                changeEnergy(player, -rules.refreshCost, Source.Market)
                renewMarket()
                emit(GameEvent.MarketRefreshed(player.id))
            }
            is GameCommand.ActivatePower -> activatePower(player, command.card)
            is GameCommand.PlayAction -> playAction(player, command)
            is GameCommand.AnswerInfinity -> {
                state.pending = null
                damageQueue.first().infinityAnswer = command.use
                processDamage()
            }
            is GameCommand.AnswerFlee -> {
                state.pending = null
                if (command.flee) {
                    state.tokyoId = null
                    emit(GameEvent.LeftTokyo(player.id))
                }
                continueAfterGroup()
            }
            is GameCommand.EndTurn -> endTurn()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Turn flow
    // ---------------------------------------------------------------------------------------------

    private fun chooseFirstPlayer(): Int {
        var candidates = state.players.map { it.id }
        while (candidates.size > 1) {
            val claws = candidates.associateWith { (1..6).count { randomFace() == DieFace.CLAW } }
            val best = claws.values.max()
            candidates = candidates.filter { claws[it] == best }
        }
        return candidates.first()
    }

    /** Start of turn: counters reset, heals first, then gains, then mandatory losses. */
    private fun beginTurn() {
        val player = state.active
        state.turnNumber++
        state.phase = Phase.ROLLING
        state.dice = List(6) { Die(DieFace.ONE) }
        state.rollsUsed = 0
        state.maxRolls = rules.baseRolls + if (player.has(TileId.SOURNOIS)) 1 else 0
        state.cyberDieIndex = null
        state.resolvedGroups.clear()
        state.clawsResolved = false
        state.triplesResolved.clear()
        state.divergentDrained.clear()

        player.tenShadowsUsed = false
        player.incantationUsed = false
        player.cyberBrainUsed = false
        player.infinityUsed = false
        player.specialGradeUsed = false
        player.cyberBrainReady = player.has(TileId.CYBERCERVEAU)
        player.divergentFistActive = player.divergentFistNextTurn
        player.divergentFistNextTurn = false

        emit(GameEvent.TurnStarted(player.id))

        if (player.has(TileId.ETERNEL)) heal(player, 1, Source.Tile(TileId.ETERNEL))
        if (player.has(TileId.INFATIGABLE)) changeEnergy(player, 1, Source.Tile(TileId.INFATIGABLE))
        if (state.tokyoId == player.id) {
            if (state.initialPlayerCount == 2) changeEnergy(player, rules.tokyoStartEnergyTwoPlayers, Source.TokyoStart)
            else changeVp(player, rules.tokyoStartVp, Source.TokyoStart)
        }
        if (player.has(CardId.SUKUNA_FINGER)) {
            changeVp(player, 1, Source.Card(CardId.SUKUNA_FINGER))
            loseHp(player, 1, Source.Card(CardId.SUKUNA_FINGER))
        }
        if (!isOver && !player.alive) endTurn()
    }

    private fun roll(player: PlayerState) {
        val first = !state.hasRolled
        state.dice = state.dice.map { die ->
            if (first || !die.kept) Die(randomFace(), kept = false) else die
        }
        state.rollsUsed++
        emit(GameEvent.Rolled(player.id, state.rollsUsed, state.dice.map { it.face }))
    }

    private fun randomFace() = DieFace.entries[random.nextInt(DieFace.entries.size)]

    private fun setFace(index: Int, face: DieFace) {
        state.dice = state.dice.mapIndexed { i, die -> if (i == index) die.copy(face = face) else die }
    }

    /** Count of each face, the Cybercerveau die being counted twice. */
    fun faceCounts(): Map<DieFace, Int> {
        val counts = DieFace.entries.associateWith { 0 }.toMutableMap()
        state.dice.forEachIndexed { index, die ->
            counts[die.face] = counts.getValue(die.face) + if (index == state.cyberDieIndex) 2 else 1
        }
        return counts
    }

    fun remainingGroups(): List<ResolveGroup> =
        ResolveGroup.entries.filter { group ->
            group !in state.resolvedGroups && state.dice.any { it.face.group() == group }
        }

    private fun resolve(player: PlayerState, group: ResolveGroup) {
        state.resolvedGroups += group
        val counts = faceCounts()
        when (group) {
            ResolveGroup.NUMBERS -> resolveNumbers(player, counts)
            ResolveGroup.ENERGY -> {
                val energy = counts.getValue(DieFace.ENERGY)
                changeEnergy(player, energy, Source.Dice)
                if (player.has(TileId.RAYON_STELLAIRE)) changeEnergy(player, energy, Source.Tile(TileId.RAYON_STELLAIRE))
            }
            ResolveGroup.HEARTS -> {
                // Dice never heal in Tokyo; Binding Vow forbids dice heals (Rayon stellaire included).
                if (state.tokyoId != player.id && !player.has(CardId.BINDING_VOW)) {
                    val hearts = counts.getValue(DieFace.HEART)
                    heal(player, hearts, Source.Dice)
                    if (player.has(TileId.RAYON_STELLAIRE)) heal(player, hearts, Source.Tile(TileId.RAYON_STELLAIRE))
                }
            }
            ResolveGroup.CLAWS -> {
                resolveClaws(player, counts.getValue(DieFace.CLAW))
                return // continues once the damage (and possible reactions) are settled
            }
        }
        continueAfterGroup()
    }

    private fun resolveNumbers(player: PlayerState, counts: Map<DieFace, Int>) {
        for (face in listOf(DieFace.ONE, DieFace.TWO, DieFace.THREE)) {
            val count = counts.getValue(face)
            if (count < 3) continue
            val value = face.number!!
            state.triplesResolved += value
            changeVp(player, value + (count - 3), Source.Triple)
            if (value == 1 && player.has(TileId.FURTIF)) changeVp(player, 1, Source.Tile(TileId.FURTIF))
            val wickedness = when (value) {
                1 -> rules.wickednessForTripleOnes
                2 -> rules.wickednessForTripleTwos
                else -> 0
            }
            if (wickedness > 0) {
                player.wickedness += wickedness
                emit(GameEvent.WickednessGained(player.id, wickedness))
            }
        }
    }

    private fun resolveClaws(attacker: PlayerState, claws: Int) {
        if (claws > 0) state.clawsResolved = true
        val inTokyo = state.tokyoId == attacker.id
        val targets = if (inTokyo) {
            state.alivePlayers.filter { it.id != attacker.id }
        } else {
            listOfNotNull(state.tokyoId?.let { state.player(it) }).filter { it.alive && it.id != attacker.id }
        }
        val damage = DamageCalculator.clawDamage(rules, attacker, claws, inTokyo)
        val bypass = DamageCalculator.bypassesDefenses(rules, attacker, claws, inTokyo)
        tokyoWoundedByClaws = false
        if (damage > 0) {
            targets.forEach {
                damageQueue += DamagePacket(attacker.id, it.id, damage, Source.Dice, fromClaws = true, bypassDefenses = bypass)
            }
        }
        afterDamage = AfterDamage.CLAWS
        processDamage()
    }

    /** Applies queued damage in order; pauses when a target may react with Infinity. */
    private fun processDamage() {
        while (damageQueue.isNotEmpty()) {
            if (isOver) {
                damageQueue.clear()
                return
            }
            val packet = damageQueue.first()
            val target = state.player(packet.targetId)
            if (!target.alive) {
                damageQueue.removeFirst()
                continue
            }
            if (packet.infinityAnswer == null && canUseInfinity(target, packet)) {
                state.pending = PendingChoice.Infinity(target.id, packet.attackerId, packet.amount)
                return
            }
            damageQueue.removeFirst()
            applyDamage(packet, target)
        }
        if (isOver) return
        when (afterDamage) {
            AfterDamage.NOTHING -> Unit
            AfterDamage.CLAWS -> {
                afterDamage = AfterDamage.NOTHING
                val occupant = state.tokyoId
                if (tokyoWoundedByClaws && occupant != null && occupant != state.activeId && state.player(occupant).alive) {
                    state.pending = PendingChoice.Flee(occupant, state.activeId)
                } else {
                    continueAfterGroup()
                }
            }
        }
    }

    private fun canUseInfinity(target: PlayerState, packet: DamagePacket) =
        target.has(CardId.INFINITY) && !target.infinityUsed && target.energy >= rules.infinityCost &&
            !packet.bypassDefenses && packet.amount > 0

    private fun applyDamage(packet: DamagePacket, target: PlayerState) {
        if (packet.infinityAnswer == true) {
            target.infinityUsed = true
            changeEnergy(target, -rules.infinityCost, Source.Card(CardId.INFINITY))
            emit(GameEvent.DamageCancelled(target.id, Source.Card(CardId.INFINITY)))
            return
        }
        var damage = packet.amount
        if (!packet.bypassDefenses) {
            if (target.has(CardId.VEIL) && state.tokyoId == target.id && damage >= rules.veilThreshold) {
                damage -= rules.veilReduction
                emit(GameEvent.DamageReduced(target.id, rules.veilReduction, Source.Card(CardId.VEIL)))
            }
            if (target.has(CardId.SPECIAL_GRADE) && !target.specialGradeUsed && damage > 0) {
                target.specialGradeUsed = true
                val reduction = minOf(damage, rules.specialGradeReduction)
                damage -= reduction
                emit(GameEvent.DamageReduced(target.id, reduction, Source.Card(CardId.SPECIAL_GRADE)))
            }
        }
        if (damage <= 0) return

        emit(GameEvent.Damaged(packet.attackerId, target.id, damage, packet.source))
        if (packet.fromClaws && state.tokyoId == target.id) tokyoWoundedByClaws = true

        val attacker = state.player(packet.attackerId)
        if (attacker.divergentFistActive && target.id !in state.divergentDrained && target.energy > 0) {
            state.divergentDrained += target.id
            changeEnergy(target, -1, Source.Card(CardId.DIVERGENT_FIST))
        }
        loseHp(target, damage, null)
    }

    /** After a group: either the next group, or the end of the resolution. */
    private fun continueAfterGroup() {
        if (isOver || state.pending != null) return
        if (remainingGroups().isEmpty()) {
            state.phase = Phase.CHOOSE_TILE
            continueAfterResolution()
        }
    }

    /** After all the dice: Wickedness tiles (lowest level first), mandatory Tokyo entry, then purchases. */
    private fun continueAfterResolution() {
        val player = state.active
        for (level in rules.wickednessLevels) {
            if (player.wickedness < level || level in player.usedTileLevels) continue
            val options = state.tilePool.filter { it.level == level }
            if (options.isEmpty()) continue // threshold reached with an empty reserve: nothing, not deferred
            state.pending = PendingChoice.ChooseTile(player.id, level, options)
            return
        }
        if (state.tokyoId == null && player.alive) enterTokyo(player, withBonus = true)
        state.phase = Phase.BUY
    }

    private fun acquireTile(player: PlayerState, tile: TileId) {
        state.tilePool -= tile
        player.usedTileLevels += tile.level
        player.tiles += tile
        emit(GameEvent.TileAcquired(player.id, tile))
        when (tile) {
            TileId.REGENERATION_TOTALE -> {
                player.maxHp = rules.boostedMaxHp
                heal(player, player.maxHp, Source.Tile(tile))
            }
            TileId.PANIQUE_GENERALE -> {
                state.alivePlayers.filter { it.id != player.id }.forEach {
                    changeVp(it, -rules.generalPanicVpLoss, Source.Tile(tile))
                }
                player.tiles -= tile
            }
            TileId.CYBERCERVEAU -> player.cyberBrainReady = false
            else -> Unit
        }
    }

    private fun enterTokyo(player: PlayerState, withBonus: Boolean) {
        state.tokyoId = player.id
        emit(GameEvent.EnteredTokyo(player.id))
        if (!withBonus) return
        if (state.initialPlayerCount == 2) changeEnergy(player, rules.tokyoEntryEnergyTwoPlayers, Source.TokyoEntry)
        else changeVp(player, rules.tokyoEntryVp, Source.TokyoEntry)
    }

    private fun endTurn() {
        val player = state.active
        if (player.alive && player.has(CardId.NANAMI_OVERTIME) && !state.clawsResolved) {
            changeEnergy(player, 1, Source.Card(CardId.NANAMI_OVERTIME))
            changeVp(player, 1, Source.Card(CardId.NANAMI_OVERTIME))
        }
        if (checkVictory()) return
        var next = player.id
        do {
            next = (next + 1) % state.players.size
        } while (!state.player(next).alive)
        state.activeId = next
        beginTurn()
    }

    /** Victory at the end of a turn: 20 points while alive, or last survivor. */
    private fun checkVictory(): Boolean {
        val alive = state.alivePlayers
        val winner = when {
            alive.isEmpty() -> null
            alive.size == 1 -> alive.first()
            else -> alive.filter { it.vp >= rules.victoryPoints }
                .sortedWith(compareByDescending<PlayerState> { it.id == state.activeId }.thenByDescending { it.vp })
                .firstOrNull() ?: return false
        }
        gameOver(winner?.id)
        return true
    }

    private fun gameOver(winnerId: Int?) {
        state.phase = Phase.GAME_OVER
        state.pending = null
        state.winnerId = winnerId
        damageQueue.clear()
        emit(GameEvent.GameOver(winnerId, winnerId != null && state.player(winnerId).isHuman))
    }

    private fun eliminate(player: PlayerState) {
        player.alive = false
        player.hp = 0
        player.energy = 0
        state.discard += player.hand
        state.discard += player.powers
        player.hand.clear()
        player.powers.clear()
        player.tiles.clear()
        if (state.tokyoId == player.id) state.tokyoId = null
        emit(GameEvent.Eliminated(player.id))
        // Game Over as soon as the human is eliminated.
        if (player.isHuman) gameOver(null)
    }

    // ---------------------------------------------------------------------------------------------
    // Cards and market
    // ---------------------------------------------------------------------------------------------

    /** Pure read: the deck is kept non-empty after each command (see [submit]). */
    private fun peekDeck(): CardId? = state.deck.firstOrNull()

    private fun draw(): CardId? {
        if (state.deck.isEmpty()) reshuffleDiscard()
        return state.deck.removeFirstOrNull()
    }

    private fun reshuffleDiscard() {
        val cards = state.discard.toMutableList()
        state.discard.clear()
        random.shuffle(cards)
        state.deck.addAll(cards)
    }

    private fun refillMarket() {
        while (state.market.size < rules.marketSize) {
            state.market += draw() ?: return
        }
    }

    private fun renewMarket() {
        state.discard += state.market
        state.market.clear()
        refillMarket()
    }

    private fun buy(player: PlayerState, card: CardId, fromMarket: Boolean) {
        val price = priceOf(player, card)
        player.energy -= price
        if (fromMarket) state.market -= card else state.deck.removeFirst()
        player.hand += card
        emit(GameEvent.CardBought(player.id, card, price))
        if (card == CardId.MAHORAGA_ADAPTATION) heal(player, rules.mahoragaHeal, Source.Card(card))
        refillMarket()
    }

    private fun activatePower(player: PlayerState, card: CardId) {
        player.hand -= card
        player.powers += card
        emit(GameEvent.PowerActivated(player.id, card))
        if (card == CardId.MAHORAGA_ADAPTATION) player.maxHp = rules.boostedMaxHp
    }

    private fun playAction(player: PlayerState, command: GameCommand.PlayAction) {
        val card = command.card
        player.hand -= card
        emit(GameEvent.ActionPlayed(player.id, card))
        val source = Source.Card(card)
        when (card) {
            CardId.REVERSE_CURSED_TECHNIQUE -> heal(player, rules.reverseCursedHeal, source)
            CardId.OVERFLOWING_ENERGY -> heal(player, rules.overflowingHeal, source)
            CardId.DIVERGENT_FIST -> player.divergentFistNextTurn = true
            CardId.TALISMAN -> {
                renewMarket()
                changeEnergy(player, 1, source)
            }
            CardId.RESONANCE -> {
                val target = state.player(command.target!!)
                val drained = target.energy
                changeEnergy(target, -drained, source)
                changeEnergy(player, (drained + 1) / 2, source)
            }
            CardId.BOOGIE_WOOGIE -> {
                // The one who leaves Tokyo always gains the point; the one who enters gets no entry bonus.
                val other = state.player(command.target!!)
                val (leaving, entering) = if (state.tokyoId == player.id) player to other else other to player
                emit(GameEvent.LeftTokyo(leaving.id))
                changeVp(leaving, rules.boogieWoogieVp, source)
                enterTokyo(entering, withBonus = false)
            }
            CardId.BLACK_FLASH -> state.alivePlayers.filter { it.id != player.id }.forEach {
                queueCardDamage(player, it.id, rules.blackFlashDamage, card)
            }
            CardId.VIOLET -> if (command.target != null) {
                queueCardDamage(player, command.target, rules.violetDamage, card)
            } else {
                command.allocation.forEach { (target, damage) -> queueCardDamage(player, target, damage, card) }
            }
            CardId.BLOOD_MANIPULATION -> {
                loseHp(player, rules.bloodManipulationCost, source)
                queueCardDamage(player, command.target!!, rules.bloodManipulationDamage, card)
            }
            else -> error("$card is not an Action")
        }
        // Action recycling (decision of A_CLARIFIER.md): discarded after resolution, reshuffled later.
        state.discard += card
        afterDamage = AfterDamage.NOTHING
        processDamage()
    }

    /** Card damage keeps its written value: no offensive bonus, defenses apply per target. */
    private fun queueCardDamage(attacker: PlayerState, targetId: Int, amount: Int, card: CardId) {
        damageQueue += DamagePacket(attacker.id, targetId, amount, Source.Card(card), fromClaws = false, bypassDefenses = false)
    }

    // ---------------------------------------------------------------------------------------------
    // Resources (always clamped: no negative value, HP capped at the maximum)
    // ---------------------------------------------------------------------------------------------

    private fun heal(player: PlayerState, amount: Int, source: Source) {
        val healed = minOf(amount, player.maxHp - player.hp).coerceAtLeast(0)
        if (healed == 0) return
        player.hp += healed
        emit(GameEvent.HpChanged(player.id, healed, source))
    }

    /** Direct loss of HP (damage already computed, or a cost). Eliminates at 0. */
    private fun loseHp(player: PlayerState, amount: Int, source: Source?) {
        if (amount <= 0 || !player.alive) return
        player.hp = (player.hp - amount).coerceAtLeast(0)
        if (source != null) emit(GameEvent.HpChanged(player.id, -amount, source))
        if (player.hp == 0) eliminate(player)
    }

    private fun changeEnergy(player: PlayerState, delta: Int, source: Source) {
        val real = if (delta < 0) -minOf(-delta, player.energy) else delta
        if (real == 0) return
        player.energy += real
        emit(GameEvent.EnergyChanged(player.id, real, source))
    }

    private fun changeVp(player: PlayerState, delta: Int, source: Source) {
        val real = if (delta < 0) -minOf(-delta, player.vp) else delta
        if (real == 0) return
        player.vp += real
        emit(GameEvent.VpChanged(player.id, real, source))
    }

    private fun emit(event: GameEvent) {
        events += event
    }
}
