package fr.epita.thecullinggames.business.engine

/**
 * Every number of the rules in one place, so balancing does not require touching the engine.
 * Damage values follow the provisional conventions of docs/A_CLARIFIER.md.
 */
data class RulesConfig(
    val startHp: Int = 10,
    val baseMaxHp: Int = 10,
    val boostedMaxHp: Int = 12,
    val victoryPoints: Int = 20,
    val baseRolls: Int = 3,
    val maxRollsCap: Int = 5,
    val marketSize: Int = 3,
    val refreshCost: Int = 2,

    // Tokyo bonuses: VP for a game started with 3-4 players, Energy for a game started with 2.
    val tokyoEntryVp: Int = 1,
    val tokyoStartVp: Int = 2,
    val tokyoEntryEnergyTwoPlayers: Int = 1,
    val tokyoStartEnergyTwoPlayers: Int = 1,

    // Wickedness thresholds and gains.
    val wickednessLevels: List<Int> = listOf(3, 6, 10),
    val wickednessForTripleOnes: Int = 2,
    val wickednessForTripleTwos: Int = 1,

    // Damage pipeline: base → ×antimatter → +fixed bonuses → Infinity → Veil → Special Grade.
    val clawDamage: Int = 1,
    val antimatterMultiplier: Int = 2,
    val domainExpansionBonus: Int = 1,
    val domainExpansionBypassClaws: Int = 3,
    val bindingVowBonus: Int = 2,
    val infinityCost: Int = 2,
    val veilThreshold: Int = 3,
    val veilReduction: Int = 1,
    val specialGradeReduction: Int = 2,

    // Card values, as written on the cards.
    val reverseCursedHeal: Int = 4,
    val overflowingHeal: Int = 3,
    val mahoragaHeal: Int = 2,
    val violetDamage: Int = 2,
    val violetTokyoTotal: Int = 3,
    val blackFlashDamage: Int = 2,
    val bloodManipulationDamage: Int = 2,
    val bloodManipulationCost: Int = 1,
    val boogieWoogieVp: Int = 1,
    val maleficLairDiscount: Int = 1,
    val generalPanicVpLoss: Int = 4,
)
