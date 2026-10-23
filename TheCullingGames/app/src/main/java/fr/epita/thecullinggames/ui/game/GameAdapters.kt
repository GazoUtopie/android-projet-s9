package fr.epita.thecullinggames.ui.game

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.epita.thecullinggames.R
import fr.epita.thecullinggames.business.model.CardId
import fr.epita.thecullinggames.databinding.ItemCardBinding
import fr.epita.thecullinggames.databinding.ItemLogBinding
import fr.epita.thecullinggames.databinding.ItemPlayerBinding
import fr.epita.thecullinggames.ui.common.Visuals

private class SameItem<T>(private val key: (T) -> Any) : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T & Any, newItem: T & Any) = key(oldItem) == key(newItem)
    override fun areContentsTheSame(oldItem: T & Any, newItem: T & Any) = oldItem == newItem
}

/** Player strip: portrait, HP bar, resources, Tokyo badge; the active player is outlined. */
class PlayerAdapter : ListAdapter<PlayerUi, PlayerAdapter.Holder>(SameItem { it.id }) {

    class Holder(val binding: ItemPlayerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemPlayerBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val player = getItem(position)
        val context = holder.itemView.context
        with(holder.binding) {
            portrait.setImageResource(Visuals.portrait(player.characterId))
            name.text = player.name
            role.setText(if (player.isHuman) R.string.you else R.string.ai)
            hpBar.max = player.maxHp
            hpBar.progress = player.hp
            stats.text = context.getString(R.string.player_stats, player.hp, player.maxHp, player.energy, player.vp)
            badge.text = when {
                !player.alive -> context.getString(R.string.eliminated)
                player.inTokyo -> context.getString(R.string.tokyo_badge)
                else -> ""
            }
            root.alpha = if (player.alive) 1f else 0.4f
            val stroke = when {
                player.isActive -> R.color.points_gold
                player.inTokyo -> R.color.wickedness_red
                else -> R.color.cursed_outline
            }
            card.strokeColor = ContextCompat.getColor(context, stroke)
            card.strokeWidth = (context.resources.displayMetrics.density * if (player.isActive) 3 else 1).toInt()
        }
    }
}

data class CardItemUi(val card: CardId, val price: Int?, val enabled: Boolean)

/** Card thumbnails, used for the market and for the hand. */
class CardAdapter(
    private val onClick: (CardItemUi) -> Unit,
) : ListAdapter<CardItemUi, CardAdapter.Holder>(SameItem { it.card }) {

    class Holder(val binding: ItemCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        val context = holder.itemView.context
        val visual = Visuals.card(item.card)
        with(holder.binding) {
            image.setImageResource(visual.image)
            name.setText(visual.name)
            price.text = item.price?.let { context.getString(R.string.card_price, it) } ?: ""
            price.visibility = if (item.price == null) android.view.View.GONE else android.view.View.VISIBLE
            price.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, if (item.enabled) R.color.points_gold else R.color.text_secondary),
            )
            image.alpha = if (item.enabled) 1f else 0.55f
            card.strokeColor = ContextCompat.getColor(context, if (item.enabled) R.color.cursed_purple else R.color.cursed_outline)
            card.setOnClickListener { onClick(item) }
        }
    }
}

/** One line of the combat log; [id] is the absolute position of the event in the game. */
data class LogLine(val id: Int, val text: String, val turnStart: Boolean)

/** Combat log, newest line first. */
class LogAdapter : ListAdapter<LogLine, LogAdapter.Holder>(SameItem { it.id }) {
    class Holder(val binding: ItemLogBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val line = getItem(position)
        holder.binding.text.text = line.text
        holder.binding.text.setTextColor(
            ContextCompat.getColor(holder.itemView.context, if (line.turnStart) R.color.points_gold else R.color.text_primary),
        )
    }
}
