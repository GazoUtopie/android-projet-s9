package fr.epita.thecullinggames.ui.common

import android.content.Context
import fr.epita.thecullinggames.R
import fr.epita.thecullinggames.business.model.GameEvent
import fr.epita.thecullinggames.business.model.Source
import kotlin.math.abs

/** Turns business events into readable French sentences for the combat log. */
class EventFormatter(private val context: Context, private val names: (Int) -> String) {

    private fun s(id: Int, vararg args: Any) = context.getString(id, *args)

    fun source(source: Source): String = when (source) {
        Source.Dice -> s(R.string.source_dice)
        Source.Triple -> s(R.string.source_triple)
        Source.TokyoEntry -> s(R.string.source_tokyo_entry)
        Source.TokyoStart -> s(R.string.source_tokyo_start)
        Source.Market -> s(R.string.source_market)
        is Source.Card -> s(Visuals.card(source.id).name)
        is Source.Tile -> s(Visuals.tileName(source.id))
    }

    fun format(event: GameEvent): String = when (event) {
        is GameEvent.FirstPlayer -> s(R.string.log_first_player, names(event.playerId))
        is GameEvent.TurnStarted -> s(R.string.log_turn, names(event.playerId))
        is GameEvent.Rolled -> s(
            R.string.log_rolled, names(event.playerId), event.rollNumber,
            event.faces.joinToString(" ") { Visuals.symbol(it) },
        )
        is GameEvent.DieChanged -> s(R.string.log_die_changed, names(event.playerId), source(event.source), Visuals.symbol(event.face))
        is GameEvent.VpChanged -> s(
            if (event.delta > 0) R.string.log_vp_gain else R.string.log_vp_loss,
            names(event.playerId), abs(event.delta), source(event.source),
        )
        is GameEvent.EnergyChanged -> s(
            if (event.delta > 0) R.string.log_energy_gain else R.string.log_energy_loss,
            names(event.playerId), abs(event.delta), source(event.source),
        )
        is GameEvent.HpChanged -> s(
            if (event.delta > 0) R.string.log_heal else R.string.log_hp_loss,
            names(event.playerId), abs(event.delta), source(event.source),
        )
        is GameEvent.WickednessGained -> s(R.string.log_wickedness, names(event.playerId), event.amount)
        is GameEvent.Damaged -> s(
            R.string.log_damage, names(event.attackerId), event.amount, names(event.targetId),
            if (event.source == Source.Dice) s(R.string.source_claws) else source(event.source),
        )
        is GameEvent.DamageCancelled -> s(R.string.log_cancelled, names(event.targetId), source(event.source))
        is GameEvent.DamageReduced -> s(R.string.log_reduced, names(event.targetId), event.amount, source(event.source))
        is GameEvent.EnteredTokyo -> s(R.string.log_enter_tokyo, names(event.playerId))
        is GameEvent.LeftTokyo -> s(R.string.log_leave_tokyo, names(event.playerId))
        is GameEvent.TileAcquired -> s(R.string.log_tile, names(event.playerId), s(Visuals.tileName(event.tile)))
        is GameEvent.CardBought -> s(R.string.log_bought, names(event.playerId), s(Visuals.card(event.card).name), event.price)
        is GameEvent.MarketRefreshed -> s(R.string.log_refresh, names(event.playerId))
        is GameEvent.PowerActivated -> s(R.string.log_power, names(event.playerId), s(Visuals.card(event.card).name))
        is GameEvent.ActionPlayed -> s(R.string.log_action, names(event.playerId), s(Visuals.card(event.card).name))
        is GameEvent.Eliminated -> s(R.string.log_eliminated, names(event.playerId))
        is GameEvent.GameOver -> event.winnerId?.let { s(R.string.log_game_over_winner, names(it)) }
            ?: s(R.string.log_game_over_none)
    }
}
