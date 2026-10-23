package fr.epita.thecullinggames.ui.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.epita.thecullinggames.R
import fr.epita.thecullinggames.business.model.CardId
import fr.epita.thecullinggames.business.model.CardType
import fr.epita.thecullinggames.business.model.GameCommand
import fr.epita.thecullinggames.business.model.GameEvent
import fr.epita.thecullinggames.business.model.PendingChoice
import fr.epita.thecullinggames.business.model.Phase
import fr.epita.thecullinggames.business.model.ResolveGroup
import fr.epita.thecullinggames.business.model.TileId
import fr.epita.thecullinggames.data.ServiceLocator
import fr.epita.thecullinggames.databinding.DialogCardBinding
import fr.epita.thecullinggames.databinding.FragmentGameBinding
import fr.epita.thecullinggames.ui.common.DieView
import fr.epita.thecullinggames.ui.common.EventFormatter
import fr.epita.thecullinggames.ui.common.Visuals
import kotlinx.coroutines.launch

/**
 * Game screen. It only renders [GameUiState] and sends [GameCommand]s to the ViewModel:
 * every control is enabled from the legal commands given by the engine.
 */
class GameFragment : Fragment() {

    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!

    private val args: GameFragmentArgs by navArgs()
    private val viewModel: GameViewModel by viewModels()

    private val playerAdapter = PlayerAdapter()
    private val marketAdapter = CardAdapter { showCard(it.card, fromMarket = true) }
    private val handAdapter = CardAdapter { showCard(it.card, fromMarket = false) }
    private val logAdapter = LogAdapter()

    private lateinit var formatter: EventFormatter
    private lateinit var dice: List<DieView>

    /** Dialog opened for a decision the engine waits for (reaction, tile). */
    private var pendingDialog: AlertDialog? = null
    private var shownPending: PendingChoice? = null

    private val state get() = viewModel.state.value

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        formatter = EventFormatter(requireContext()) { id -> state.players.firstOrNull { it.id == id }?.name ?: "?" }
        val board = binding.board
        val cards = binding.cards
        board.players.adapter = playerAdapter
        cards.market.adapter = marketAdapter
        cards.hand.adapter = handAdapter
        binding.logPanel.log.adapter = logAdapter

        dice = listOf(board.die0, board.die1, board.die2, board.die3, board.die4, board.die5)
        dice.forEachIndexed { index, die ->
            die.setOnClickListener { sendIfLegal(GameCommand.ToggleKeep(state.humanId, index)) }
        }

        board.btnRoll.setOnClickListener { sendIfLegal(GameCommand.Roll(state.humanId)) }
        board.btnStop.setOnClickListener { sendIfLegal(GameCommand.StopRolling(state.humanId)) }
        board.btnIncantation.setOnClickListener { sendIfLegal(GameCommand.UseIncantation(state.humanId)) }
        board.btnTenShadows.setOnClickListener { chooseDie(state.legal.filterIsInstance<GameCommand.UseTenShadows>()) { it.dieIndex } }
        board.btnCyber.setOnClickListener { chooseDie(state.legal.filterIsInstance<GameCommand.UseCyberBrain>()) { it.dieIndex } }
        mapOf(
            board.btnResolveNumbers to ResolveGroup.NUMBERS,
            board.btnResolveEnergy to ResolveGroup.ENERGY,
            board.btnResolveHearts to ResolveGroup.HEARTS,
            board.btnResolveClaws to ResolveGroup.CLAWS,
        ).forEach { (button, group) ->
            button.setOnClickListener { sendIfLegal(GameCommand.Resolve(state.humanId, group)) }
        }
        cards.btnRefresh.setOnClickListener { sendIfLegal(GameCommand.RefreshMarket(state.humanId)) }
        cards.btnDeckTop.setOnClickListener { state.deckTop?.let { showCard(it, fromMarket = true, deckTop = true) } }
        cards.btnEndTurn.setOnClickListener { sendIfLegal(GameCommand.EndTurn(state.humanId)) }

        setupMenuAndBack()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { render(it) } }
                launch {
                    viewModel.result.collect { result ->
                        if (result != null && findNavController().currentDestination?.id == R.id.gameFragment) {
                            findNavController().navigate(
                                GameFragmentDirections.actionGameFragmentToGameOverFragment(
                                    humanWon = result.humanWon,
                                    humanVp = result.humanVp,
                                    humanHp = result.humanHp,
                                    turns = result.turns,
                                    characterId = args.characterId,
                                    winnerName = result.winnerName,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun sendIfLegal(command: GameCommand) {
        if (command in state.legal) viewModel.send(command)
    }

    // ---------------------------------------------------------------------------------------------
    // Rendering
    // ---------------------------------------------------------------------------------------------

    private fun render(ui: GameUiState) {
        val board = binding.board
        val cards = binding.cards
        val myTurn = ui.isHumanDecision && ui.humanPending == null

        playerAdapter.submitList(ui.players)

        val occupant = ui.tokyoId?.let { ui.player(it) }
        board.tokyoPortrait.isVisible = occupant != null
        occupant?.let { board.tokyoPortrait.setImageResource(Visuals.portrait(it.characterId)) }
        board.tokyoName.text = occupant?.name ?: getString(R.string.tokyo_empty)

        board.status.text = statusText(ui)

        // Dice: shown for whoever is playing, so the human can follow the AIs.
        dice.forEachIndexed { index, view ->
            val die = ui.dice.getOrNull(index)
            view.face = if (ui.hasRolled) die?.face else null
            view.kept = die?.kept == true && ui.phase == Phase.ROLLING
            view.doubled = ui.cyberDieIndex == index
            view.isEnabled = GameCommand.ToggleKeep(ui.humanId, index) in ui.legal
        }
        board.tapHint.isVisible = myTurn && ui.phase == Phase.ROLLING && ui.hasRolled

        val legal = ui.legal
        board.btnRoll.isVisible = myTurn && ui.phase == Phase.ROLLING
        board.btnRoll.isEnabled = GameCommand.Roll(ui.humanId) in legal
        board.btnRoll.setText(if (ui.hasRolled) R.string.action_reroll else R.string.action_roll)
        board.btnStop.isVisible = myTurn && ui.phase == Phase.ROLLING
        board.btnStop.isEnabled = GameCommand.StopRolling(ui.humanId) in legal
        board.btnIncantation.isVisible = GameCommand.UseIncantation(ui.humanId) in legal
        board.btnTenShadows.isVisible = legal.any { it is GameCommand.UseTenShadows }
        board.btnCyber.isVisible = legal.any { it is GameCommand.UseCyberBrain }

        board.resolveRow.isVisible = myTurn && ui.phase == Phase.RESOLVING
        board.btnResolveNumbers.isEnabled = GameCommand.Resolve(ui.humanId, ResolveGroup.NUMBERS) in legal
        board.btnResolveEnergy.isEnabled = GameCommand.Resolve(ui.humanId, ResolveGroup.ENERGY) in legal
        board.btnResolveHearts.isEnabled = GameCommand.Resolve(ui.humanId, ResolveGroup.HEARTS) in legal
        board.btnResolveClaws.isEnabled = GameCommand.Resolve(ui.humanId, ResolveGroup.CLAWS) in legal

        board.wickednessLabel.text = getString(R.string.section_wickedness, ui.wickedness)
        board.wickednessGauge.value = ui.wickedness

        marketAdapter.submitList(ui.market.map { CardItemUi(it.card, it.price, it.buyable) })
        cards.btnRefresh.text = getString(R.string.action_refresh, ui.refreshCost)
        cards.btnRefresh.isEnabled = GameCommand.RefreshMarket(ui.humanId) in legal
        cards.btnDeckTop.isVisible = ui.deckTop != null

        handAdapter.submitList(ui.hand.distinct().map { card -> CardItemUi(card, null, legal.any { it.usesCard(card) }) })
        cards.handEmpty.isVisible = ui.hand.isEmpty()

        renderPowers(ui)
        cards.btnEndTurn.isEnabled = GameCommand.EndTurn(ui.humanId) in legal

        val total = ui.logSize
        logAdapter.submitList(
            ui.log.mapIndexed { i, event -> LogLine(total - i, formatter.format(event), event is GameEvent.TurnStarted) },
        ) { _binding?.logPanel?.log?.scrollToPosition(0) } // the diff ends asynchronously, the view may be gone

        handlePending(ui.humanPending)
    }

    private fun statusText(ui: GameUiState): String {
        if (ui.isOver) return getString(R.string.status_over)
        val decider = ui.deciderId?.let { ui.player(it) }
        if (decider != null && !decider.isHuman) return getString(R.string.status_ai_turn, decider.name)
        if (ui.humanPending != null && ui.activeId != ui.humanId) {
            return getString(R.string.status_waiting_you, ui.player(ui.activeId).name)
        }
        return when (ui.phase) {
            Phase.ROLLING -> if (ui.hasRolled) getString(R.string.status_rolling, ui.rollsUsed, ui.maxRolls)
            else getString(R.string.status_roll)
            Phase.RESOLVING -> getString(R.string.status_resolving)
            Phase.CHOOSE_TILE -> getString(R.string.status_tile)
            Phase.BUY -> getString(R.string.status_buy)
            Phase.GAME_OVER -> getString(R.string.status_over)
        }
    }

    private fun renderPowers(ui: GameUiState) {
        val group = binding.cards.powers
        group.removeAllViews()
        ui.powers.forEach { card ->
            group.addView(Chip(requireContext()).apply {
                setText(Visuals.card(card).name)
                setOnClickListener { showCard(card, fromMarket = false) }
            })
        }
        ui.tiles.forEach { tile ->
            group.addView(Chip(requireContext()).apply {
                setText(Visuals.tileName(tile))
                setChipBackgroundColorResource(R.color.cursed_purple_dark)
                setOnClickListener { showTile(tile) }
            })
        }
        binding.cards.powersEmpty.isVisible = ui.powers.isEmpty() && ui.tiles.isEmpty()
    }

    private fun GameCommand.usesCard(card: CardId) =
        (this is GameCommand.ActivatePower && this.card == card) || (this is GameCommand.PlayAction && this.card == card)

    // ---------------------------------------------------------------------------------------------
    // Decisions the engine waits for (Infinity, flee, tile)
    // ---------------------------------------------------------------------------------------------

    private fun handlePending(pending: PendingChoice?) {
        if (pending == shownPending) return
        pendingDialog?.dismiss()
        pendingDialog = null
        shownPending = pending
        if (pending == null) return
        val humanId = state.humanId
        val builder = MaterialAlertDialogBuilder(requireContext()).setCancelable(false)
        pendingDialog = when (pending) {
            is PendingChoice.Flee -> builder
                .setTitle(R.string.dialog_flee_title)
                .setMessage(getString(R.string.dialog_flee_message, state.player(pending.attackerId).name))
                .setPositiveButton(R.string.dialog_flee_yes) { _, _ -> viewModel.send(GameCommand.AnswerFlee(humanId, true)) }
                .setNegativeButton(R.string.dialog_flee_no) { _, _ -> viewModel.send(GameCommand.AnswerFlee(humanId, false)) }
                .show()
            is PendingChoice.Infinity -> builder
                .setTitle(R.string.dialog_infinity_title)
                .setMessage(getString(R.string.dialog_infinity_message, state.player(pending.attackerId).name, pending.damage))
                .setPositiveButton(R.string.action_yes) { _, _ -> viewModel.send(GameCommand.AnswerInfinity(humanId, true)) }
                .setNegativeButton(R.string.action_no) { _, _ -> viewModel.send(GameCommand.AnswerInfinity(humanId, false)) }
                .show()
            is PendingChoice.ChooseTile -> {
                val labels = pending.options.map { "${getString(Visuals.tileName(it))} — ${getString(Visuals.tileDescription(it))}" }
                builder.setTitle(getString(R.string.dialog_tile_title, pending.level))
                    .setItems(labels.toTypedArray()) { _, which ->
                        viewModel.send(GameCommand.ChooseTile(humanId, pending.options[which]))
                    }
                    .show()
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Cards, dice choices and targets
    // ---------------------------------------------------------------------------------------------

    private fun showCard(card: CardId, fromMarket: Boolean, deckTop: Boolean = false) {
        val ui = state
        val visual = Visuals.card(card)
        val def = ServiceLocator.cardRepository.all().first { it.id == card }
        val content = DialogCardBinding.inflate(layoutInflater)
        content.image.setImageResource(visual.image)
        val type = getString(if (def.type == CardType.POWER) R.string.card_type_power else R.string.card_type_action)
        content.meta.text = getString(R.string.card_cost_type, def.cost, type)
        content.description.setText(visual.description)

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (deckTop) "${getString(R.string.dialog_deck_top)} : ${getString(visual.name)}" else getString(visual.name))
            .setView(content.root)
            .setNegativeButton(R.string.action_close, null)

        if (fromMarket) {
            val buy = if (deckTop) GameCommand.BuyTopOfDeck(ui.humanId) else GameCommand.BuyCard(ui.humanId, card)
            val price = if (deckTop) ui.deckTopPrice else ui.market.firstOrNull { it.card == card }?.price ?: def.cost
            if (buy in ui.legal) builder.setPositiveButton(getString(R.string.action_buy, price)) { _, _ -> viewModel.send(buy) }
        } else {
            val uses = ui.legal.filter { it.usesCard(card) }
            val activate = uses.filterIsInstance<GameCommand.ActivatePower>().firstOrNull()
            val plays = uses.filterIsInstance<GameCommand.PlayAction>()
            when {
                activate != null -> builder.setPositiveButton(R.string.action_activate) { _, _ -> viewModel.send(activate) }
                plays.size == 1 && plays[0].target == null && plays[0].allocation.isEmpty() ->
                    builder.setPositiveButton(R.string.action_play) { _, _ -> viewModel.send(plays[0]) }
                plays.isNotEmpty() -> builder.setPositiveButton(R.string.action_play) { _, _ -> chooseTarget(plays) }
            }
        }
        builder.show()
    }

    private fun chooseTarget(options: List<GameCommand.PlayAction>) {
        val ui = state
        val split = options.any { it.allocation.isNotEmpty() }
        val labels = options.map { option ->
            if (option.allocation.isNotEmpty()) {
                option.allocation.entries.joinToString(getString(R.string.split_separator)) { (id, damage) ->
                    getString(R.string.split_item, ui.player(id).name, damage)
                }
            } else {
                option.target?.let { ui.player(it).name } ?: ""
            }
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (split) R.string.dialog_split_title else R.string.dialog_target_title)
            .setItems(labels.toTypedArray()) { _, which -> viewModel.send(options[which]) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun <T : GameCommand> chooseDie(options: List<T>, index: (T) -> Int) {
        if (options.isEmpty()) return
        val labels = options.map {
            val i = index(it)
            getString(R.string.dialog_die_item, i + 1, getString(Visuals.faceName(state.dice[i].face)))
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_die_title)
            .setItems(labels.toTypedArray()) { _, which -> viewModel.send(options[which]) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showTile(tile: TileId) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(Visuals.tileName(tile))
            .setMessage("${getString(R.string.tile_level, tile.level)}\n\n${getString(Visuals.tileDescription(tile))}")
            .setPositiveButton(R.string.action_close, null)
            .show()
    }

    // ---------------------------------------------------------------------------------------------
    // Toolbar menu and back navigation (leaving loses the game: there is no save)
    // ---------------------------------------------------------------------------------------------

    private fun setupMenuAndBack() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.add(Menu.NONE, MENU_RULES, Menu.NONE, R.string.action_rules)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
                MENU_RULES -> {
                    findNavController().navigate(GameFragmentDirections.actionGameFragmentToRulesFragment())
                    true
                }
                android.R.id.home -> {
                    confirmQuit()
                    true
                }
                else -> false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = confirmQuit()
        })
    }

    private fun confirmQuit() {
        if (state.isOver) {
            findNavController().popBackStack()
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_quit_title)
            .setMessage(R.string.dialog_quit_message)
            .setPositiveButton(R.string.dialog_quit_yes) { _, _ -> findNavController().popBackStack() }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pendingDialog?.dismiss()
        pendingDialog = null
        shownPending = null
        _binding = null
    }

    private companion object {
        const val MENU_RULES = 1
    }
}
