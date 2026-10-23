package fr.epita.thecullinggames.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import fr.epita.thecullinggames.R
import fr.epita.thecullinggames.business.model.CardType
import fr.epita.thecullinggames.data.ServiceLocator
import fr.epita.thecullinggames.databinding.FragmentRulesBinding
import fr.epita.thecullinggames.databinding.ItemRuleEntryBinding
import fr.epita.thecullinggames.ui.common.Visuals

/** Help screen: summary of the rules, then every card and every Wickedness tile. */
class RulesFragment : Fragment() {

    private var _binding: FragmentRulesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRulesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        for (card in ServiceLocator.cardRepository.all()) {
            val visual = Visuals.card(card.id)
            val entry = ItemRuleEntryBinding.inflate(layoutInflater, binding.cardsContainer, true)
            entry.image.setImageResource(visual.image)
            entry.name.setText(visual.name)
            val type = getString(if (card.type == CardType.POWER) R.string.card_type_power else R.string.card_type_action)
            entry.meta.text = getString(R.string.card_cost_type, card.cost, type)
            entry.description.setText(visual.description)
        }
        for (tile in ServiceLocator.tileRepository.all()) {
            val entry = ItemRuleEntryBinding.inflate(layoutInflater, binding.tilesContainer, true)
            entry.image.visibility = View.GONE
            entry.name.setText(Visuals.tileName(tile))
            entry.meta.text = getString(R.string.tile_level, tile.level)
            entry.description.setText(Visuals.tileDescription(tile))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
