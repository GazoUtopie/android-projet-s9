package fr.epita.thecullinggames.data

import fr.epita.thecullinggames.business.model.CardDef
import fr.epita.thecullinggames.business.model.CardId
import fr.epita.thecullinggames.business.model.CardType
import fr.epita.thecullinggames.business.model.Character
import fr.epita.thecullinggames.business.model.TileId
import fr.epita.thecullinggames.business.repository.CardRepository
import fr.epita.thecullinggames.business.repository.CharacterRepository
import fr.epita.thecullinggames.business.repository.TileRepository

/** The four sorcerers of the Culling Games. The player picks one, the others become the AIs. */
class CharacterCatalog : CharacterRepository {
    private val characters = listOf(
        Character("gojo", "Satoru Gojo"),
        Character("megumi", "Megumi Fushiguro"),
        Character("hakari", "Kinji Hakari"),
        Character("jogo", "Jogo"),
    )

    override fun all() = characters
    override fun byId(id: String) = characters.first { it.id == id }
}

/** Prices and types of the 20 Energy cards, as written in docs/Rules-Cards (one copy each). */
class CardCatalog : CardRepository {
    private val cards = listOf(
        CardDef(CardId.DOMAIN_EXPANSION, 5, CardType.POWER),
        CardDef(CardId.INFINITY, 5, CardType.POWER),
        CardDef(CardId.REVERSE_CURSED_TECHNIQUE, 5, CardType.ACTION),
        CardDef(CardId.BINDING_VOW, 5, CardType.POWER),
        CardDef(CardId.MAHORAGA_ADAPTATION, 5, CardType.POWER),
        CardDef(CardId.SPECIAL_GRADE, 4, CardType.POWER),
        CardDef(CardId.BLACK_FLASH, 4, CardType.ACTION),
        CardDef(CardId.VIOLET, 4, CardType.ACTION),
        CardDef(CardId.VEIL, 4, CardType.POWER),
        CardDef(CardId.RESONANCE, 4, CardType.ACTION),
        CardDef(CardId.NANAMI_OVERTIME, 3, CardType.POWER),
        CardDef(CardId.TEN_SHADOWS, 3, CardType.POWER),
        CardDef(CardId.SUKUNA_FINGER, 3, CardType.POWER),
        CardDef(CardId.OVERFLOWING_ENERGY, 3, CardType.ACTION),
        CardDef(CardId.INCANTATION, 3, CardType.POWER),
        CardDef(CardId.DIVERGENT_FIST, 2, CardType.ACTION),
        CardDef(CardId.BOOGIE_WOOGIE, 2, CardType.ACTION),
        CardDef(CardId.MAKI_GLASSES, 2, CardType.POWER),
        CardDef(CardId.BLOOD_MANIPULATION, 2, CardType.ACTION),
        CardDef(CardId.TALISMAN, 2, CardType.ACTION),
    )

    override fun all() = cards
}

/** Common reserves of Wickedness tiles: four at level 3, four at level 6, two at level 10. */
class TileCatalog : TileRepository {
    override fun all() = TileId.entries.toList()
}
