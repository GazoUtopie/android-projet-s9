package fr.epita.thecullinggames.business.model

/**
 * Stable identifiers shared by Business, Data and View.
 * Business never depends on Android resources: the View maps these ids to texts and images.
 */

/** The 20 custom Energy cards described in docs/Rules-Cards. */
enum class CardId {
    DOMAIN_EXPANSION,
    INFINITY,
    REVERSE_CURSED_TECHNIQUE,
    BINDING_VOW,
    MAHORAGA_ADAPTATION,
    SPECIAL_GRADE,
    BLACK_FLASH,
    VIOLET,
    VEIL,
    RESONANCE,
    NANAMI_OVERTIME,
    TEN_SHADOWS,
    SUKUNA_FINGER,
    OVERFLOWING_ENERGY,
    INCANTATION,
    DIVERGENT_FIST,
    BOOGIE_WOOGIE,
    MAKI_GLASSES,
    BLOOD_MANIPULATION,
    TALISMAN,
}

/** The 10 Wickedness tiles described in docs/KOT_WICKEDNESS.md, with their threshold level. */
enum class TileId(val level: Int) {
    INFATIGABLE(3),
    ETERNEL(3),
    FURTIF(3),
    SOURNOIS(3),
    REGENERATION_TOTALE(6),
    PANIQUE_GENERALE(6),
    REPAIRE_MALEFIQUE(6),
    CYBERCERVEAU(6),
    RAYON_STELLAIRE(10),
    RAYON_ANTIMATIERE(10),
}

enum class CardType { POWER, ACTION }

/** Rules data of an Energy card (price and type). Texts and images live in the View layer. */
data class CardDef(val id: CardId, val cost: Int, val type: CardType)

/** A playable monster. [id] is stable and used by the View to find the portrait. */
data class Character(val id: String, val name: String)
