package fr.epita.thecullinggames.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import fr.epita.thecullinggames.databinding.FragmentHomeBinding

/** Home screen: start a game or read the rules. */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnPlay.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToCharacterSelectFragment())
        }
        binding.btnRules.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToRulesFragment())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
