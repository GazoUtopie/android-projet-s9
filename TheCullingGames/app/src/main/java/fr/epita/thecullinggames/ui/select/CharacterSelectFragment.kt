package fr.epita.thecullinggames.ui.select

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import fr.epita.thecullinggames.R
import fr.epita.thecullinggames.business.model.Character
import fr.epita.thecullinggames.databinding.FragmentCharacterSelectBinding
import fr.epita.thecullinggames.databinding.ItemCharacterBinding
import fr.epita.thecullinggames.ui.common.Visuals
import kotlinx.coroutines.launch

/** Character selection: the player picks a sorcerer and the number of AI opponents. */
class CharacterSelectFragment : Fragment() {

    private var _binding: FragmentCharacterSelectBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CharacterSelectViewModel by viewModels()
    private val adapter = CharacterAdapter { viewModel.select(it.id) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCharacterSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val columns = if (resources.configuration.screenWidthDp >= 600) 4 else 2
        binding.characters.layoutManager = GridLayoutManager(requireContext(), columns)
        binding.characters.adapter = adapter

        binding.aiCount.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            viewModel.setAiCount(
                when (checkedId) {
                    R.id.ai_1 -> 1
                    R.id.ai_2 -> 2
                    else -> 3
                },
            )
        }

        binding.btnStart.setOnClickListener {
            val state = viewModel.state.value
            val selected = state.selectedId
            if (selected == null) {
                Snackbar.make(binding.root, R.string.select_pick_first, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            findNavController().navigate(
                CharacterSelectFragmentDirections.actionCharacterSelectFragmentToGameFragment(selected, state.aiCount),
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    adapter.submit(state.characters, state.selectedId)
                    binding.aiCount.check(
                        when (state.aiCount) {
                            1 -> R.id.ai_1
                            2 -> R.id.ai_2
                            else -> R.id.ai_3
                        },
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private class CharacterAdapter(
    private val onClick: (Character) -> Unit,
) : RecyclerView.Adapter<CharacterAdapter.Holder>() {

    private var items: List<Character> = emptyList()
    private var selectedId: String? = null

    class Holder(val binding: ItemCharacterBinding) : RecyclerView.ViewHolder(binding.root)

    fun submit(characters: List<Character>, selected: String?) {
        items = characters
        selectedId = selected
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemCharacterBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val character = items[position]
        with(holder.binding) {
            portrait.setImageResource(Visuals.portrait(character.id))
            name.text = character.name
            subtitle.setText(Visuals.characterTitle(character.id))
            card.isChecked = character.id == selectedId
            val density = root.resources.displayMetrics.density
            card.strokeWidth = (density * if (card.isChecked) 3 else 1).toInt()
            card.strokeColor = ContextCompat.getColor(root.context, if (card.isChecked) R.color.points_gold else R.color.cursed_outline)
            card.setOnClickListener { onClick(character) }
        }
    }
}
