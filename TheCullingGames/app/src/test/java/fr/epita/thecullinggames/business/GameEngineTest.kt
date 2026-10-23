package fr.epita.thecullinggames.business

import fr.epita.thecullinggames.business.engine.GameEngine
import fr.epita.thecullinggames.business.engine.KotlinRandomSource
import fr.epita.thecullinggames.business.model.CardId
import fr.epita.thecullinggames.business.model.Die
import fr.epita.thecullinggames.business.model.DieFace
import fr.epita.thecullinggames.business.model.DieFace.CLAW
import fr.epita.thecullinggames.business.model.DieFace.ENERGY
import fr.epita.thecullinggames.business.model.DieFace.HEART
import fr.epita.thecullinggames.business.model.DieFace.ONE
import fr.epita.thecullinggames.business.model.DieFace.THREE
import fr.epita.thecullinggames.business.model.DieFace.TWO
import fr.epita.thecullinggames.business.model.GameCommand
import fr.epita.thecullinggames.business.model.PendingChoice
import fr.epita.thecullinggames.business.model.Phase
import fr.epita.thecullinggames.business.model.ResolveGroup
import fr.epita.thecullinggames.business.model.TileId
import fr.epita.thecullinggames.data.CardCatalog
import fr.epita.thecullinggames.data.CharacterCatalog
import fr.epita.thecullinggames.data.TileCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    private fun newGame(players: Int = 3, seed: Long = 7) = GameEngine(
        CharacterCatalog().all().take(players),
        CardCatalog().all(),
        TileCatalog().all(),
        KotlinRandomSource(seed),
    )

    private val GameEngine.me get() = state.activeId

    private fun GameEngine.ok(command: GameCommand) {
        val result = submit(command)
        assertTrue(result.error ?: "", result.accepted)
    }

    /** Rolls once, forces the faces, then stops rolling. */
    private fun GameEngine.rollFaces(vararg faces: DieFace) {
        ok(GameCommand.Roll(me))
        state.dice = faces.map { Die(it) }
        ok(GameCommand.StopRolling(me))
    }

    private fun GameEngine.resolve(group: ResolveGroup) = ok(GameCommand.Resolve(me, group))

    private fun GameEngine.resolveAll() {
        while (state.phase == Phase.RESOLVING && state.pending == null) {
            ok(GameCommand.Resolve(me, remainingGroups().first()))
        }
    }

    private fun GameEngine.other(offset: Int = 1) = state.player((me + offset) % state.players.size)

    @Test
    fun `triple of threes gives three points plus one per extra die`() {
        val game = newGame()
        val player = game.state.active
        game.rollFaces(THREE, THREE, THREE, THREE, ENERGY, ENERGY)
        game.resolveAll()
        assertEquals(4, player.vp - (if (game.state.tokyoId == player.id) 1 else 0))
        assertEquals(2, player.energy)
    }

    @Test
    fun `triple of ones gives wickedness and offers a tile at threshold 3`() {
        val game = newGame()
        val player = game.state.active
        player.wickedness = 1
        game.rollFaces(ONE, ONE, ONE, ENERGY, ENERGY, HEART)
        game.resolveAll()
        assertEquals(3, player.wickedness)
        val pending = game.state.pending as PendingChoice.ChooseTile
        assertEquals(4, pending.options.size)
        game.ok(GameCommand.ChooseTile(player.id, TileId.SOURNOIS))
        assertTrue(player.has(TileId.SOURNOIS))
        assertEquals(3, player.wickedness) // not spent
        assertEquals(Phase.BUY, game.state.phase)
    }

    @Test
    fun `claws from outside hit Tokyo, the occupant may flee and the attacker enters`() {
        val game = newGame()
        val attacker = game.state.active
        val occupant = game.other()
        game.state.tokyoId = occupant.id
        game.rollFaces(CLAW, CLAW, ENERGY, ENERGY, TWO, THREE)
        game.resolve(ResolveGroup.CLAWS)
        assertEquals(8, occupant.hp)
        assertEquals(PendingChoice.Flee(occupant.id, attacker.id), game.state.pending)
        game.ok(GameCommand.AnswerFlee(occupant.id, true))
        game.resolveAll()
        assertEquals(attacker.id, game.state.tokyoId)
        assertEquals(1, attacker.vp)
    }

    @Test
    fun `game started with two players gives Energy instead of points in Tokyo`() {
        val game = newGame(players = 2)
        val player = game.state.active
        game.rollFaces(TWO, TWO, THREE, THREE, ONE, ONE)
        game.resolveAll()
        assertEquals(player.id, game.state.tokyoId)
        assertEquals(1, player.energy)
        assertEquals(0, player.vp)
    }

    @Test
    fun `dice do not heal in Tokyo`() {
        val game = newGame()
        val player = game.state.active
        game.state.tokyoId = player.id
        player.hp = 5
        game.rollFaces(HEART, HEART, HEART, ENERGY, TWO, THREE)
        game.resolveAll()
        assertEquals(5, player.hp)
    }

    @Test
    fun `Binding Vow forbids dice heals but adds two damage`() {
        val game = newGame()
        val player = game.state.active
        val occupant = game.other()
        player.powers += CardId.BINDING_VOW
        player.hp = 5
        game.state.tokyoId = occupant.id
        game.rollFaces(HEART, HEART, CLAW, ENERGY, TWO, THREE)
        game.resolveAll()
        assertEquals(5, player.hp)
        assertEquals(7, occupant.hp)
    }

    @Test
    fun `Infinity cancels an attack for two Energy`() {
        val game = newGame()
        val occupant = game.other()
        occupant.powers += CardId.INFINITY
        occupant.energy = 2
        game.state.tokyoId = occupant.id
        game.rollFaces(CLAW, CLAW, CLAW, ENERGY, TWO, THREE)
        game.resolve(ResolveGroup.CLAWS)
        assertTrue(game.state.pending is PendingChoice.Infinity)
        game.ok(GameCommand.AnswerInfinity(occupant.id, true))
        assertEquals(10, occupant.hp)
        assertEquals(0, occupant.energy)
        assertNull(game.state.pending) // not wounded: no flee offered
    }

    @Test
    fun `Domain Expansion with three claws from Tokyo ignores defenses`() {
        val game = newGame()
        val attacker = game.state.active
        val target = game.other()
        attacker.powers += CardId.DOMAIN_EXPANSION
        target.powers += CardId.SPECIAL_GRADE
        target.powers += CardId.INFINITY
        target.energy = 5
        game.state.tokyoId = attacker.id
        game.rollFaces(CLAW, CLAW, CLAW, ENERGY, TWO, THREE)
        game.resolve(ResolveGroup.CLAWS)
        assertNull(game.state.pending)
        assertEquals(6, target.hp) // 3 claws + 1 Domain Expansion
    }

    @Test
    fun `Veil then Special Grade reduce an attack in Tokyo`() {
        val game = newGame()
        val attacker = game.state.active
        val occupant = game.other()
        attacker.powers += CardId.BINDING_VOW
        occupant.powers += CardId.VEIL
        occupant.powers += CardId.SPECIAL_GRADE
        game.state.tokyoId = occupant.id
        game.rollFaces(CLAW, ENERGY, ENERGY, ENERGY, TWO, THREE)
        game.resolve(ResolveGroup.CLAWS) // 1 + 2 = 3 → Veil 2 → Special Grade 0
        assertEquals(10, occupant.hp)
        assertTrue(occupant.specialGradeUsed)
    }

    @Test
    fun `elimination of the human ends the game immediately`() {
        val game = newGame(seed = 3)
        // Make sure an AI is active and the human sits in Tokyo.
        while (game.state.active.isHuman) {
            game.rollFaces(TWO, TWO, THREE, THREE, ONE, ENERGY)
            game.resolveAll()
            game.ok(GameCommand.EndTurn(game.me))
        }
        val human = game.state.human
        game.state.tokyoId = human.id
        human.hp = 2
        game.rollFaces(CLAW, CLAW, CLAW, ENERGY, TWO, THREE)
        game.resolve(ResolveGroup.CLAWS)
        assertFalse(human.alive)
        assertEquals(Phase.GAME_OVER, game.state.phase)
        assertNull(game.state.winnerId)
    }

    @Test
    fun `twenty points win at the end of the turn`() {
        val game = newGame()
        val player = game.state.active
        player.vp = 19
        game.rollFaces(ONE, ONE, ONE, ENERGY, TWO, THREE)
        game.resolveAll()
        game.state.pending?.let { game.ok(GameCommand.ChooseTile(player.id, (it as PendingChoice.ChooseTile).options.first())) }
        assertEquals(Phase.BUY, game.state.phase) // not won yet: checked at the end of the turn
        game.ok(GameCommand.EndTurn(player.id))
        assertEquals(Phase.GAME_OVER, game.state.phase)
        assertEquals(player.id, game.state.winnerId)
    }

    @Test
    fun `Eternel heals before Sukuna's finger at the start of the turn`() {
        val game = newGame()
        val next = game.other()
        next.tiles += TileId.ETERNEL
        next.powers += CardId.SUKUNA_FINGER
        next.hp = 1
        game.rollFaces(TWO, TWO, THREE, THREE, ONE, ENERGY)
        game.resolveAll()
        game.ok(GameCommand.EndTurn(game.me))
        assertEquals(next.id, game.state.activeId)
        assertTrue(next.alive)
        assertEquals(1, next.hp)
        assertEquals(1, next.vp)
    }

    @Test
    fun `Cybercerveau counts one die twice, including for triples`() {
        val game = newGame()
        val player = game.state.active
        player.tiles += TileId.CYBERCERVEAU
        player.cyberBrainReady = true
        game.rollFaces(TWO, TWO, ENERGY, ENERGY, HEART, THREE)
        game.ok(GameCommand.UseCyberBrain(player.id, 0))
        game.resolve(ResolveGroup.NUMBERS)
        assertEquals(2, player.vp)
        assertEquals(1, player.wickedness)
    }

    @Test
    fun `Sournois and Incantation allow up to five rolls`() {
        val game = newGame()
        val player = game.state.active
        player.tiles += TileId.SOURNOIS
        player.powers += CardId.INCANTATION
        player.energy = 1
        game.state.maxRolls = 4 // as computed at the start of the turn with Sournois
        repeat(4) { game.ok(GameCommand.Roll(player.id)) }
        assertFalse(GameCommand.Roll(player.id) in game.legalCommands(player.id))
        game.ok(GameCommand.UseIncantation(player.id))
        game.ok(GameCommand.Roll(player.id))
        assertEquals(5, game.state.rollsUsed)
        assertEquals(0, player.energy)
    }

    @Test
    fun `kept dice are not rerolled and can be taken back`() {
        val game = newGame()
        val player = game.state.active
        game.ok(GameCommand.Roll(player.id))
        game.ok(GameCommand.ToggleKeep(player.id, 0))
        val kept = game.state.dice[0]
        game.ok(GameCommand.Roll(player.id))
        assertEquals(kept, game.state.dice[0])
        game.ok(GameCommand.ToggleKeep(player.id, 0))
        assertFalse(game.state.dice[0].kept)
    }

    @Test
    fun `buying replaces the card, Repaire malefique lowers the price`() {
        val game = newGame()
        val player = game.state.active
        player.tiles += TileId.REPAIRE_MALEFIQUE
        game.rollFaces(TWO, TWO, THREE, THREE, ONE, ENERGY)
        game.resolveAll()
        player.energy = 10
        val card = game.state.market.first()
        game.ok(GameCommand.BuyCard(player.id, card))
        assertEquals(10 - (game.cardDef(card).cost - 1), player.energy)
        assertTrue(card in player.hand)
        assertEquals(3, game.state.market.size)
        assertFalse(card in game.state.market)
    }

    @Test
    fun `Mahoraga heals on purchase and raises the maximum on activation`() {
        val game = newGame()
        val player = game.state.active
        game.rollFaces(TWO, TWO, THREE, THREE, ONE, ENERGY)
        game.resolveAll()
        game.state.market[0] = CardId.MAHORAGA_ADAPTATION
        player.energy = 5
        player.hp = 7
        game.ok(GameCommand.BuyCard(player.id, CardId.MAHORAGA_ADAPTATION))
        assertEquals(9, player.hp)
        game.ok(GameCommand.ActivatePower(player.id, CardId.MAHORAGA_ADAPTATION))
        assertEquals(12, player.maxHp)
    }

    @Test
    fun `used Actions go to the discard pile`() {
        val game = newGame()
        val player = game.state.active
        player.hand += CardId.OVERFLOWING_ENERGY
        player.hp = 5
        game.ok(GameCommand.PlayAction(player.id, CardId.OVERFLOWING_ENERGY))
        assertEquals(8, player.hp)
        assertTrue(CardId.OVERFLOWING_ENERGY in game.state.discard)
        assertFalse(CardId.OVERFLOWING_ENERGY in player.hand)
    }

    @Test
    fun `Boogie Woogie swaps with the occupant who gains a point`() {
        val game = newGame()
        val player = game.state.active
        val occupant = game.other()
        game.state.tokyoId = occupant.id
        player.hand += CardId.BOOGIE_WOOGIE
        game.ok(GameCommand.PlayAction(player.id, CardId.BOOGIE_WOOGIE, target = occupant.id))
        assertEquals(player.id, game.state.tokyoId)
        assertEquals(1, occupant.vp)
        assertEquals(0, player.vp)
    }

    @Test
    fun `Boogie Woogie from Tokyo sends an opponent in, the leaver gains a point`() {
        val game = newGame()
        val player = game.state.active
        val other = game.other()
        game.state.tokyoId = player.id
        player.hand += CardId.BOOGIE_WOOGIE
        game.ok(GameCommand.PlayAction(player.id, CardId.BOOGIE_WOOGIE, target = other.id))
        assertEquals(other.id, game.state.tokyoId)
        assertEquals(1, player.vp)
        assertEquals(0, other.vp)
    }

    @Test
    fun `market is refilled as soon as the discard pile is reshuffled`() {
        val game = newGame()
        val player = game.state.active
        game.rollFaces(TWO, TWO, THREE, THREE, ONE, ENERGY)
        game.resolveAll()
        game.state.deck.clear()
        game.state.discard.clear()
        player.energy = 10
        game.ok(GameCommand.BuyCard(player.id, game.state.market.first()))
        assertEquals(2, game.state.market.size) // nothing left to draw
        val heal = listOf(CardId.OVERFLOWING_ENERGY, CardId.REVERSE_CURSED_TECHNIQUE).first { it !in game.state.market }
        player.hand += heal
        game.ok(GameCommand.PlayAction(player.id, heal))
        assertEquals(3, game.state.market.size)
        assertTrue(heal in game.state.market)
    }

    @Test
    fun `Boogie Woogie is not playable when Tokyo is empty`() {
        val game = newGame()
        val player = game.state.active
        player.hand += CardId.BOOGIE_WOOGIE
        assertTrue(game.legalCommands(player.id).none { it is GameCommand.PlayAction })
    }

    @Test
    fun `Divergent Fist drains one Energy per target during the next turn`() {
        val game = newGame()
        val player = game.state.active
        val occupant = game.other()
        player.divergentFistActive = true
        occupant.energy = 3
        game.state.tokyoId = occupant.id
        game.rollFaces(CLAW, CLAW, ENERGY, ENERGY, TWO, THREE)
        game.resolve(ResolveGroup.CLAWS)
        assertEquals(2, occupant.energy)
    }

    @Test
    fun `Panique generale removes four points from the others then is discarded`() {
        val game = newGame()
        val player = game.state.active
        val other = game.other()
        other.vp = 6
        player.wickedness = 6
        player.usedTileLevels += 3
        game.rollFaces(TWO, TWO, THREE, THREE, ONE, ENERGY)
        game.resolveAll()
        game.ok(GameCommand.ChooseTile(player.id, TileId.PANIQUE_GENERALE))
        assertEquals(2, other.vp)
        assertFalse(player.has(TileId.PANIQUE_GENERALE))
        assertTrue(6 in player.usedTileLevels)
    }

    @Test
    fun `illegal commands are refused`() {
        val game = newGame()
        val other = game.other()
        assertFalse(game.submit(GameCommand.Roll(other.id)).accepted)
        assertFalse(game.submit(GameCommand.EndTurn(game.me)).accepted)
    }
}
