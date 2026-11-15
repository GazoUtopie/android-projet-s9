package fr.epita.thecullinggames.ui.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
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
import fr.epita.thecullinggames.business.model.PurchaseBlock
import fr.epita.thecullinggames.business.model.TargetOption
import fr.epita.thecullinggames.business.model.ResolveGroup
import fr.epita.thecullinggames.business.model.TileId
import fr.epita.thecullinggames.data.ServiceLocator
import fr.epita.thecullinggames.databinding.DialogCardBinding
import fr.epita.thecullinggames.databinding.DialogPlayerBinding
import fr.epita.thecullinggames.databinding.PopupPeekBinding
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

    private val playerAdapter = PlayerAdapter { showPlayer(it.id) }
    private val marketAdapter = CardAdapter({ showCard(it.card, fromMarket = true) }, { item, view -> peekCard(item.card, view) })
    private val logAdapter = LogAdapter()

    private lateinit var formatter: EventFormatter
    private lateinit var dice: List<DieView>
    private lateinit var feedback: FeedbackPresenter
    private var peek: PopupWindow? = null

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

        feedback = FeedbackPresenter(
            overlay = binding.feedback,
            tokyoCard = board.tokyoCard,
            formatter = formatter,
            playerPanel = ::playerPanel,
            portraitOf = { id -> Visuals.portrait(state.player(id).characterId) },
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { render(it) } }
                launch {
                    viewModel.feedback.collect { item ->
                        // An item already started before a rotation is shown still, never replayed.
                        feedback.show(item, animate = item != null && item.id != viewModel.displayedFeedbackId)
                        item?.let { viewModel.markFeedbackDisplayed(it.id) }
                    }
                }
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
            view.toReroll = die?.kept == false && ui.hasRolled && ui.phase == Phase.ROLLING
            view.doubled = ui.cyberDieIndex == index
            view.isEnabled = GameCommand.ToggleKeep(ui.humanId, index) in ui.legal
        }
        board.tapHint.isVisible = myTurn && ui.phase == Phase.ROLLING && ui.hasRolled

        val legal = ui.legal
        board.btnRoll.isVisible = myTurn && ui.phase == Phase.ROLLING
        board.btnRoll.isEnabled = GameCommand.Roll(ui.humanId) in legal
        board.btnRoll.text = if (ui.hasRolled) {
            getString(R.string.action_reroll, ui.dice.count { !it.kept })
        } else {
            getString(R.string.action_roll)
        }
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
                setOnLongClickListener { peekCard(card, it); true }
            })
        }
        ui.tiles.forEach { tile ->
            group.addView(Chip(requireContext()).apply {
                setText(Visuals.tileName(tile))
                setChipBackgroundColorResource(R.color.cursed_purple_dark)
                setOnClickListener { showTile(tile) }
                setOnLongClickListener { peekTile(tile, it); true }
            })
        }
        binding.cards.powersEmpty.isVisible = ui.powers.isEmpty() && ui.tiles.isEmpty()
    }

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
            is PendingChoice.CardTargets -> {
                val split = pending.options.any { it.allocation.isNotEmpty() }
                builder.setTitle(getString(if (split) R.string.dialog_split_title else R.string.dialog_target_title) +
                    " — " + getString(Visuals.card(pending.card).name))
                    .setItems(pending.options.map(::targetLabel).toTypedArray()) { _, which ->
                        viewModel.send(GameCommand.ChooseCardTarget(humanId, pending.options[which]))
                    }
                    .show()
            }
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

    /** Card sheet. From the market it offers the purchase, or says why the card cannot be bought now. */
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
            val market = ui.market.firstOrNull { it.card == card }
            val price = if (deckTop) ui.deckTopPrice else market?.price ?: def.cost
            val block = if (deckTop) ui.deckTopBlock else market?.block
            if (buy in ui.legal) {
                builder.setPositiveButton(getString(R.string.action_buy, price)) { _, _ -> viewModel.send(buy) }
            } else if (block != null) {
                content.reason.isVisible = true
                content.reason.setText(blockText(block))
            }
        }
        builder.show()
    }

    private fun blockText(block: PurchaseBlock) = when (block) {
        PurchaseBlock.NOT_BUY_PHASE -> R.string.block_not_buy_phase
        PurchaseBlock.NOT_ENOUGH_ENERGY -> R.string.block_energy
        PurchaseBlock.NO_TRIPLE -> R.string.block_no_triple
        PurchaseBlock.TOKYO_EMPTY -> R.string.block_tokyo_empty
        PurchaseBlock.ONE_HP -> R.string.block_one_hp
        PurchaseBlock.NO_TARGET -> R.string.block_no_target
    }

    /** "Jogo" for a single target, "Jogo : 2 · Megumi : 1" for a split of Violet. */
    private fun targetLabel(option: TargetOption): String = if (option.allocation.isNotEmpty()) {
        option.allocation.entries.joinToString(getString(R.string.split_separator)) { (id, damage) ->
            getString(R.string.split_item, state.player(id).name, damage)
        }
    } else {
        option.target?.let { state.player(it).name } ?: ""
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
    // Player profile and feedback anchors
    // ---------------------------------------------------------------------------------------------

    /** Panel of a player in the strip, scrolled into view; null if it is not laid out yet. */
    private fun playerPanel(playerId: Int): View? {
        val strip = _binding?.board?.players ?: return null
        val index = playerAdapter.currentList.indexOfFirst { it.id == playerId }
        if (index < 0) return null
        strip.scrollToPosition(index)
        return strip.findViewHolderForAdapterPosition(index)?.itemView
    }

    private fun showPlayer(playerId: Int) {
        val player = state.player(playerId)
        val content = DialogPlayerBinding.inflate(layoutInflater)
        content.portrait.setImageResource(Visuals.portrait(player.characterId))
        content.stats.text = if (player.alive) {
            getString(R.string.player_stats, player.hp, player.maxHp, player.energy, player.vp)
        } else {
            getString(R.string.profile_dead)
        }
        content.wickednessLabel.text = getString(R.string.section_wickedness, player.wickedness)
        content.wickednessGauge.value = player.wickedness
        fillChips(content.tiles, player.tiles.map { tile ->
            Chip(requireContext()).apply {
                setText(Visuals.tileName(tile))
                setChipBackgroundColorResource(R.color.cursed_purple_dark)
                setOnClickListener { showTile(tile) }
                setOnLongClickListener { peekTile(tile, it); true }
            }
        })
        fillChips(content.powers, player.powers.map { card ->
            Chip(requireContext()).apply {
                setText(Visuals.card(card).name)
                setOnClickListener { showCard(card, fromMarket = false) }
                setOnLongClickListener { peekCard(card, it); true }
            }
        })
        content.effects.text = player.effects.takeIf { it.isNotEmpty() }
            ?.joinToString("\n") { "• " + getString(effectText(it)) }
            ?: getString(R.string.effects_none)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.profile_title, player.name, getString(if (player.isHuman) R.string.you else R.string.ai)))
            .setView(content.root)
            .setPositiveButton(R.string.action_close, null)
            .show()
    }

    private fun fillChips(group: com.google.android.material.chip.ChipGroup, chips: List<Chip>) {
        if (chips.isEmpty()) {
            group.addView(Chip(requireContext()).apply { setText(R.string.profile_none); isEnabled = false })
        } else {
            chips.forEach(group::addView)
        }
    }

    private fun effectText(effect: PlayerEffect) = when (effect) {
        PlayerEffect.DIVERGENT_FIST_NEXT_TURN -> R.string.effect_divergent_next
        PlayerEffect.DIVERGENT_FIST_ACTIVE -> R.string.effect_divergent_active
        PlayerEffect.INFINITY_USED -> R.string.effect_infinity_used
        PlayerEffect.SPECIAL_GRADE_USED -> R.string.effect_special_grade_used
        PlayerEffect.CYBER_BRAIN_NEXT_TURN -> R.string.effect_cyber_next
    }

    // ---------------------------------------------------------------------------------------------
    // Long-press preview (touch equivalent of a hover)
    // ---------------------------------------------------------------------------------------------

    private fun peekCard(card: CardId, anchor: View) {
        val visual = Visuals.card(card)
        val def = ServiceLocator.cardRepository.all().first { it.id == card }
        val type = getString(if (def.type == CardType.POWER) R.string.card_type_power else R.string.card_type_action)
        showPeek(anchor) {
            image.setImageResource(visual.image)
            name.setText(visual.name)
            meta.text = getString(R.string.card_cost_type, def.cost, type)
            description.setText(visual.description)
        }
    }

    private fun peekTile(tile: TileId, anchor: View) = showPeek(anchor) {
        image.isVisible = false
        name.setText(Visuals.tileName(tile))
        meta.text = getString(R.string.tile_level, tile.level)
        description.setText(Visuals.tileDescription(tile))
    }

    private fun showPeek(anchor: View, fill: PopupPeekBinding.() -> Unit) {
        peek?.dismiss()
        val content = PopupPeekBinding.inflate(layoutInflater).apply(fill)
        peek = PopupWindow(content.root, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            isOutsideTouchable = true
            elevation = 12 * resources.displayMetrics.density
            content.root.setOnClickListener { dismiss() }
            showAsDropDown(anchor)
        }
        val shown = peek
        anchor.postDelayed({ if (peek === shown) shown?.dismiss() }, PEEK_MS)
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
        peek?.dismiss()
        peek = null
        _binding = null
    }

    private companion object {
        const val MENU_RULES = 1
        const val PEEK_MS = 4000L
    }
}
