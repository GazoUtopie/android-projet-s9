package fr.epita.thecullinggames.business.repository

import fr.epita.thecullinggames.business.engine.GameEngine
import fr.epita.thecullinggames.business.model.CardDef
import fr.epita.thecullinggames.business.model.Character
import fr.epita.thecullinggames.business.model.TileId

/** Repositories are declared by Business and implemented by Data. */

interface CharacterRepository {
    fun all(): List<Character>
    fun byId(id: String): Character
}

interface CardRepository {
    fun all(): List<CardDef>
}

interface TileRepository {
    fun all(): List<TileId>
}

/** Holds the game in progress, in memory only (no persistent save, by specification). */
interface GameRepository {
    fun newGame(humanCharacterId: String, aiCount: Int, seed: Long? = null): GameEngine
    fun current(): GameEngine?
    fun clear()
}
