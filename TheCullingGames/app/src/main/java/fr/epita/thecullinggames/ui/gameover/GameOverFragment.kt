package fr.epita.thecullinggames.ui.gameover

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import fr.epita.thecullinggames.R
import fr.epita.thecullinggames.data.ServiceLocator
import fr.epita.thecullinggames.databinding.FragmentGameOverBinding
import fr.epita.thecullinggames.ui.common.Visuals

/** End screen: victory or defeat, then a new game or back to the home screen. */
class GameOverFragment : Fragment() {

    private var _binding: FragmentGameOverBinding? = null
    private val binding get() = _binding!!
    private val args: GameOverFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGameOverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // The game is finished: nothing to keep in memory any more.
        ServiceLocator.gameRepository.clear()

        val color = ContextCompat.getColor(requireContext(), if (args.humanWon) R.color.points_gold else R.color.wickedness_red)
        binding.portrait.setImageResource(Visuals.portrait(args.characterId))
        binding.portrait.strokeColor = ColorStateList.valueOf(color)
        binding.title.setText(if (args.humanWon) R.string.result_victory else R.string.result_defeat)
        binding.title.setTextColor(color)
        binding.message.text = when {
            args.humanWon -> getString(R.string.result_victory_message)
            args.winnerName != null -> getString(R.string.result_winner_message, args.winnerName)
            args.humanHp == 0 -> getString(R.string.result_defeat_eliminated)
            else -> getString(R.string.result_no_winner)
        }
        binding.stats.text = getString(R.string.result_stats, args.humanVp, args.humanHp, args.turns)

        binding.btnAgain.setOnClickListener {
            findNavController().navigate(GameOverFragmentDirections.actionGameOverFragmentToCharacterSelectFragment())
        }
        binding.btnHome.setOnClickListener { findNavController().popBackStack(R.id.homeFragment, false) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
