package fr.epita.thecullinggames.business

import fr.epita.thecullinggames.business.ai.SimpleAi
import fr.epita.thecullinggames.business.engine.GameEngine
import fr.epita.thecullinggames.business.engine.KotlinRandomSource
import fr.epita.thecullinggames.data.CardCatalog
import fr.epita.thecullinggames.data.CharacterCatalog
import fr.epita.thecullinggames.data.TileCatalog
import org.junit.Assert.assertTrue
import org.junit.Test

/** AIs play every seat (the human seat included): every game must end, with legal commands only. */
class AiSimulationTest {

    @Test
    fun `AI-only games always finish with legal commands`() {
        for (seed in 0L until 600L) {
            val players = 2 + (seed % 3).toInt()
            val engine = GameEngine(
                CharacterCatalog().all().take(players),
                CardCatalog().all(),
                TileCatalog().all(),
                KotlinRandomSource(seed),
            )
            val ai = SimpleAi()
            var steps = 0
            while (!engine.isOver) {
                val decider = engine.decider!!
                val command = ai.decide(engine, decider)
                val result = engine.submit(command)
                assertTrue("seed $seed: ${result.error}", result.accepted)
                // Immediate victory: a living player at 20 points always means the game is over.
                if (engine.state.alivePlayers.any { it.vp >= engine.rules.victoryPoints }) {
                    assertTrue("seed $seed: 20 points without victory", engine.isOver)
                }
                steps++
                assertTrue("seed $seed: game does not end", steps < 20_000)
            }
            engine.state.winnerId?.let { id ->
                val winner = engine.state.player(id)
                assertTrue(
                    "seed $seed: winner without 20 points nor last survivor",
                    winner.alive && (winner.vp >= engine.rules.victoryPoints || engine.state.alivePlayers.size == 1),
                )
            }
        }
    }
}
