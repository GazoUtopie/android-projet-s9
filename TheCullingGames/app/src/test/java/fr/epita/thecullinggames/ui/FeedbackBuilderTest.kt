package fr.epita.thecullinggames.ui

import fr.epita.thecullinggames.business.model.CardId
import fr.epita.thecullinggames.business.model.GameEvent
import fr.epita.thecullinggames.business.model.Source
import fr.epita.thecullinggames.business.model.TileId
import fr.epita.thecullinggames.ui.game.Feedback
import fr.epita.thecullinggames.ui.game.FeedbackBuilder
import fr.epita.thecullinggames.ui.game.Impact
import fr.epita.thecullinggames.ui.game.ImpactKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Feedback items are built from the engine events only, in order, with their impacts. */
class FeedbackBuilderTest {

    private fun build(vararg events: GameEvent): List<Feedback> {
        var id = 0L
        return FeedbackBuilder.build(events.toList()) { id++ }
    }

    @Test
    fun `buying Boogie Woogie gives one item with its effect, then the Tokyo moves`() {
        val card = Source.Card(CardId.BOOGIE_WOOGIE)
        val items = build(
            GameEvent.CardBought(0, CardId.BOOGIE_WOOGIE, 2),
            GameEvent.ActionPlayed(0, CardId.BOOGIE_WOOGIE),
            GameEvent.LeftTokyo(1),
            GameEvent.VpChanged(1, 1, card),
            GameEvent.EnteredTokyo(0),
        )
        assertEquals(3, items.size)
        val showcase = items[0] as Feedback.Showcase
        assertEquals(CardId.BOOGIE_WOOGIE, showcase.card)
        assertEquals(listOf(Impact(1, ImpactKind.VP, 1)), showcase.impacts)
        assertEquals(listOf<GameEvent>(GameEvent.VpChanged(1, 1, card)), showcase.effects)
        assertEquals(Feedback.Tokyo(1, GameEvent.LeftTokyo(1), 1, entered = false), items[1])
        assertTrue((items[2] as Feedback.Tokyo).entered)
    }

    @Test
    fun `a Power bought is shown once, and a tile showcase collects the losses it causes`() {
        val items = build(
            GameEvent.CardBought(2, CardId.VEIL, 4),
            GameEvent.TileAcquired(2, TileId.PANIQUE_GENERALE),
            GameEvent.VpChanged(0, -4, Source.Tile(TileId.PANIQUE_GENERALE)),
            GameEvent.VpChanged(1, -3, Source.Tile(TileId.PANIQUE_GENERALE)),
        )
        assertEquals(2, items.size)
        assertEquals(CardId.VEIL, (items[0] as Feedback.Showcase).card)
        val tile = items[1] as Feedback.Showcase
        assertEquals(TileId.PANIQUE_GENERALE, tile.tile)
        assertEquals(listOf(Impact(0, ImpactKind.VP, -4), Impact(1, ImpactKind.VP, -3)), tile.impacts)
    }

    @Test
    fun `an Action resolved after its target choice shows its effects only, not the card a second time`() {
        val items = build(
            GameEvent.ActionPlayed(0, CardId.VIOLET),
            GameEvent.Damaged(0, 2, 2, Source.Card(CardId.VIOLET)),
        )
        assertEquals(1, items.size)
        assertEquals(listOf(Impact(2, ImpactKind.DAMAGE, -2)), (items[0] as Feedback.Impacts).impacts)
    }

    @Test
    fun `card damage after an Infinity decision is shown on its own, start-of-turn effects are not`() {
        val items = build(
            GameEvent.Damaged(0, 2, 2, Source.Card(CardId.BLACK_FLASH)),
            GameEvent.HpChanged(1, 1, Source.Tile(TileId.ETERNEL)),
            GameEvent.Damaged(0, 3, 4, Source.Dice),
        )
        assertEquals(1, items.size)
        assertEquals(listOf(Impact(2, ImpactKind.DAMAGE, -2)), (items[0] as Feedback.Impacts).impacts)
    }
}
