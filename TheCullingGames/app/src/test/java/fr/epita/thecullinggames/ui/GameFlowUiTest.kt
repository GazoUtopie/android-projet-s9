package fr.epita.thecullinggames.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.epita.thecullinggames.MainActivity
import fr.epita.thecullinggames.R
import fr.epita.thecullinggames.business.ai.SimpleAi
import fr.epita.thecullinggames.business.model.GameCommand
import fr.epita.thecullinggames.business.model.Phase
import fr.epita.thecullinggames.business.model.ResolveGroup
import fr.epita.thecullinggames.data.ServiceLocator
import fr.epita.thecullinggames.ui.game.GameFragment
import fr.epita.thecullinggames.ui.game.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog
import android.widget.Button
import org.junit.Assert.assertFalse
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.time.Duration

/**
 * UI smoke test (Robolectric, no device needed): home → character selection → a full game
 * driven through the real buttons → end screen. Also checks that a rotation keeps the game.
 * Screenshots are written to app/build/ui-screenshots.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GameFlowUiTest {

    private val outDir = File("build/ui-screenshots").apply { mkdirs() }
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var activity: MainActivity

    private fun idle(ms: Long = 0) {
        val looper = shadowOf(Looper.getMainLooper())
        if (ms > 0) looper.idleFor(Duration.ofMillis(ms))
        looper.idle()
    }

    private fun refreshActivity() = scenario.onActivity { activity = it }

    private fun shot(name: String) {
        idle()
        val root = activity.window.decorView
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        FileOutputStream(File(outDir, "$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun click(id: Int) {
        val view = activity.findViewById<View>(id)
        assertTrue("view ${activity.resources.getResourceEntryName(id)} not clickable", view.isShown && view.isEnabled)
        view.performClick()
        idle()
    }

    private fun gameFragment(): GameFragment? {
        val host = activity.supportFragmentManager.findFragmentById(R.id.activity_main_navhost) as NavHostFragment
        return host.childFragmentManager.primaryNavigationFragment as? GameFragment
    }

    @Test
    fun `a full game can be played from the home screen to the end screen`() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        refreshActivity()
        idle()
        shot("01_home")

        click(R.id.btn_play)
        // Non-default choices, to check that the navigation arguments reach the game.
        val characters = activity.findViewById<RecyclerView>(R.id.characters)
        characters.findViewHolderForAdapterPosition(2)!!.itemView.performClick()
        click(R.id.ai_2)
        shot("02_select")
        click(R.id.btn_start)
        val started = ServiceLocator.gameRepository.current()!!.state
        assertEquals("hakari", started.human.character.id)
        assertEquals(3, started.players.size)

        val ai = SimpleAi()
        val taken = mutableSetOf<String>()
        fun shotOnce(name: String) {
            if (taken.add(name)) shot(name)
        }

        var rotated = false
        var profileChecked = false
        var sawBanner = false
        var steps = 0
        while (steps++ < 4000) {
            val engine = ServiceLocator.gameRepository.current() ?: break
            val viewModel = ViewModelProvider(gameFragment() ?: break)[GameViewModel::class.java]
            val ui = viewModel.state.value
            if (ui.isOver) break
            if (activity.findViewById<View>(R.id.banner).isShown) {
                sawBanner = true
                shotOnce("09_feedback_banner")
            }
            if (!ui.isHumanDecision) {
                idle(800) // let the AIs play
                continue
            }

            // Tapping a player of the strip opens their profile.
            if (!profileChecked) {
                profileChecked = true
                val strip = activity.findViewById<RecyclerView>(R.id.players)
                strip.findViewHolderForAdapterPosition(1)!!.itemView.performClick()
                idle()
                val profile = ShadowDialog.getLatestDialog()
                assertTrue(profile.isShowing)
                assertTrue(profile.findViewById<View>(R.id.wickedness_gauge).isShown)
                shot("10_profile")
                profile.dismiss()
                idle()
            }
            if (ui.humanPending != null) shotOnce("07_decision_dialog")
            when (ui.phase) {
                Phase.ROLLING -> if (!ui.hasRolled) shotOnce("03_game_start") else shotOnce("04_rolled")
                Phase.RESOLVING -> shotOnce("05_resolving")
                Phase.BUY -> shotOnce("06_buy")
                else -> Unit
            }

            // Rotation in the middle of the human's turn: the game must survive.
            if (!rotated && ui.phase == Phase.ROLLING && ui.hasRolled) {
                rotated = true
                // Dice selection is inverted: everything is kept, the player taps the dice to reroll.
                val roll = activity.findViewById<Button>(R.id.btn_roll)
                assertTrue(ui.dice.all { it.kept })
                assertFalse(roll.isEnabled)
                assertTrue(roll.text.toString().endsWith("(0)"))
                click(R.id.die_0)
                assertTrue(roll.isEnabled)
                assertTrue(roll.text.toString().endsWith("(1)"))
                click(R.id.die_0)
                assertFalse(roll.isEnabled)

                val rollsBefore = ui.rollsUsed
                val feedbackBefore = viewModel.feedback.value
                RuntimeEnvironment.setQualifiers("w891dp-h411dp-land-xxhdpi")
                scenario.recreate()
                refreshActivity()
                idle()
                shot("04b_rolled_landscape")
                val after = ViewModelProvider(gameFragment()!!)[GameViewModel::class.java].state.value
                assertEquals(rollsBefore, after.rollsUsed)
                // The feedback queue lives in the ViewModel: the current item is kept, not restarted.
                val vmAfter = ViewModelProvider(gameFragment()!!)[GameViewModel::class.java]
                assertEquals(feedbackBefore?.id ?: -1L, vmAfter.feedback.value?.id ?: -1L)
                feedbackBefore?.let { assertEquals(it.id, vmAfter.displayedFeedbackId) }
                assertEquals(engine, ServiceLocator.gameRepository.current())
                RuntimeEnvironment.setQualifiers("w411dp-h891dp-port-xxhdpi")
                scenario.recreate()
                refreshActivity()
                idle()
                continue
            }

            // Same decisions as an AI, but sent through the real buttons when there is one.
            when (val command = ai.decide(engine, ui.humanId)) {
                is GameCommand.Roll -> click(R.id.btn_roll)
                is GameCommand.StopRolling -> click(R.id.btn_stop)
                is GameCommand.ToggleKeep -> click(dieIds[command.dieIndex])
                is GameCommand.Resolve -> click(resolveIds.getValue(command.group))
                is GameCommand.EndTurn -> click(R.id.btn_end_turn)
                else -> {
                    viewModel.send(command)
                    idle()
                }
            }
        }

        assertTrue("game did not end", ServiceLocator.gameRepository.current()?.isOver != false)
        assertTrue("no Tokyo or purchase banner was shown", sawBanner)
        idle(2500) // result delay then navigation
        refreshActivity()
        shot("08_game_over")
        assertTrue(activity.findViewById<View>(R.id.btn_again).isShown)
        scenario.close()
    }

    private val dieIds = listOf(R.id.die_0, R.id.die_1, R.id.die_2, R.id.die_3, R.id.die_4, R.id.die_5)
    private val resolveIds = mapOf(
        ResolveGroup.NUMBERS to R.id.btn_resolve_numbers,
        ResolveGroup.ENERGY to R.id.btn_resolve_energy,
        ResolveGroup.HEARTS to R.id.btn_resolve_hearts,
        ResolveGroup.CLAWS to R.id.btn_resolve_claws,
    )
}
