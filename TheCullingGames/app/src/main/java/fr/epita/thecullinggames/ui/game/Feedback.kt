package fr.epita.thecullinggames.ui.game

import fr.epita.thecullinggames.business.model.CardId
import fr.epita.thecullinggames.business.model.GameEvent
import fr.epita.thecullinggames.business.model.Source
import fr.epita.thecullinggames.business.model.TileId

/**
 * Short, non-blocking visual feedback shown on top of the game screen. Every item is built only
 * from the [GameEvent]s produced by the engine: the View never deduces what happened by itself.
 */
sealed interface Feedback {
    /** Unique in the game: lets the View know whether an item was already displayed. */
    val id: Long
    val event: GameEvent

    /** Tokyo entry or exit: banner + highlight of the Tokyo zone and of the player's panel. */
    data class Tokyo(override val id: Long, override val event: GameEvent, val playerId: Int, val entered: Boolean) : Feedback

    /**
     * Card bought (and used at once) or tile chosen: shown large with its buyer, then sent to the
     * players it affected. [effects] are the events of its effect, written under the card.
     */
    data class Showcase(
        override val id: Long,
        override val event: GameEvent,
        val playerId: Int,
        val card: CardId?,
        val tile: TileId?,
        val impacts: List<Impact>,
        val effects: List<GameEvent>,
    ) : Feedback

    /** Effect of an Action resolved after its purchase (target chosen, Infinity decision). */
    data class Impacts(override val id: Long, override val event: GameEvent, val impacts: List<Impact>) : Feedback
}

enum class ImpactKind { DAMAGE, HEAL, HP_LOSS, ENERGY, VP }

/** A visible consequence of a card or tile on one player; [amount] is signed. */
data class Impact(val playerId: Int, val kind: ImpactKind, val amount: Int)

/** Turns the events of one engine command into feedback items, in order: one item per purchase. */
object FeedbackBuilder {

    fun build(events: List<GameEvent>, nextId: () -> Long): List<Feedback> {
        val items = mutableListOf<Feedback>()
        // Items still collecting the consequences of their card or tile, by the source that identifies them.
        val open = mutableMapOf<Source, Int>()

        fun attach(source: Source, impact: Impact?, event: GameEvent) {
            val index = open[source] ?: return
            items[index] = when (val item = items[index]) {
                is Feedback.Showcase -> item.copy(
                    impacts = item.impacts + listOfNotNull(impact),
                    effects = item.effects + event,
                )
                is Feedback.Impacts -> item.copy(impacts = item.impacts + listOfNotNull(impact))
                is Feedback.Tokyo -> item
            }
        }

        for (event in events) {
            when (event) {
                is GameEvent.EnteredTokyo -> items += Feedback.Tokyo(nextId(), event, event.playerId, entered = true)
                is GameEvent.LeftTokyo -> items += Feedback.Tokyo(nextId(), event, event.playerId, entered = false)
                is GameEvent.CardBought -> {
                    open[Source.Card(event.card)] = items.size
                    items += Feedback.Showcase(nextId(), event, event.playerId, event.card, null, emptyList(), emptyList())
                }
                // Bought in the same command: already shown. Resolved later (target chosen): its effects only.
                is GameEvent.ActionPlayed -> if (Source.Card(event.card) !in open) {
                    open[Source.Card(event.card)] = items.size
                    items += Feedback.Impacts(nextId(), event, emptyList())
                }
                is GameEvent.TileAcquired -> {
                    open[Source.Tile(event.tile)] = items.size
                    items += Feedback.Showcase(nextId(), event, event.playerId, null, event.tile, emptyList(), emptyList())
                }
                is GameEvent.Damaged -> if (event.source is Source.Card) {
                    val impact = Impact(event.targetId, ImpactKind.DAMAGE, -event.amount)
                    if (event.source in open) {
                        attach(event.source, impact, event)
                    } else {
                        // Card damage applied after an Infinity decision, in a later command.
                        items += Feedback.Impacts(nextId(), event, listOf(impact))
                    }
                }
                is GameEvent.HpChanged -> attach(
                    event.source,
                    Impact(event.playerId, if (event.delta > 0) ImpactKind.HEAL else ImpactKind.HP_LOSS, event.delta),
                    event,
                )
                is GameEvent.EnergyChanged -> attach(event.source, Impact(event.playerId, ImpactKind.ENERGY, event.delta), event)
                is GameEvent.VpChanged -> attach(event.source, Impact(event.playerId, ImpactKind.VP, event.delta), event)
                else -> Unit
            }
        }
        // An Action resolved later without any visible consequence (e.g. Divergent Fist) needs no extra item.
        return items.filterNot { it is Feedback.Impacts && it.impacts.isEmpty() }
    }
}
