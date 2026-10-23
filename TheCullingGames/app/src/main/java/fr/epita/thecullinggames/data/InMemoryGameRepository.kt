package fr.epita.thecullinggames.data

import fr.epita.thecullinggames.business.engine.GameEngine
import fr.epita.thecullinggames.business.engine.KotlinRandomSource
import fr.epita.thecullinggames.business.repository.CardRepository
import fr.epita.thecullinggames.business.repository.CharacterRepository
import fr.epita.thecullinggames.business.repository.GameRepository
import fr.epita.thecullinggames.business.repository.TileRepository

/** Keeps the current game in memory: no persistent save, by specification. */
class InMemoryGameRepository(
    private val characters: CharacterRepository,
    private val cards: CardRepository,
    private val tiles: TileRepository,
) : GameRepository {

    private var game: GameEngine? = null

    override fun newGame(humanCharacterId: String, aiCount: Int, seed: Long?): GameEngine {
        val random = KotlinRandomSource(seed)
        val human = characters.byId(humanCharacterId)
        val others = characters.all().filter { it.id != human.id }.toMutableList()
        random.shuffle(others)
        val participants = listOf(human) + others.take(aiCount.coerceIn(1, others.size))
        return GameEngine(participants, cards.all(), tiles.all(), random).also { game = it }
    }

    override fun current() = game

    override fun clear() {
        game = null
    }
}

/** Minimal manual dependency injection: one instance of each repository for the app. */
object ServiceLocator {
    val characterRepository: CharacterRepository by lazy { CharacterCatalog() }
    val cardRepository: CardRepository by lazy { CardCatalog() }
    val tileRepository: TileRepository by lazy { TileCatalog() }
    val gameRepository: GameRepository by lazy {
        InMemoryGameRepository(characterRepository, cardRepository, tileRepository)
    }
}
