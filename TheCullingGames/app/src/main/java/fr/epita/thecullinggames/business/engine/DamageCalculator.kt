package fr.epita.thecullinggames.business.engine

import fr.epita.thecullinggames.business.model.CardId
import fr.epita.thecullinggames.business.model.PlayerState
import fr.epita.thecullinggames.business.model.TileId

/**
 * Offensive part of the damage pipeline (provisional conventions of docs/A_CLARIFIER.md).
 * Defensive reductions (Infinity, Veil, Special Grade) are applied per target by the engine.
 */
object DamageCalculator {

    /**
     * Damage dealt to each target by a dice attack of [claws] Claw faces
     * (Cybercerveau already counted): base × Rayon antimatière, then fixed bonuses.
     */
    fun clawDamage(rules: RulesConfig, attacker: PlayerState, claws: Int, inTokyo: Boolean): Int {
        if (claws <= 0) return 0
        var damage = claws * rules.clawDamage
        if (attacker.has(TileId.RAYON_ANTIMATIERE)) damage *= rules.antimatterMultiplier
        if (attacker.has(CardId.DOMAIN_EXPANSION) && inTokyo) damage += rules.domainExpansionBonus
        if (attacker.has(CardId.BINDING_VOW)) damage += rules.bindingVowBonus
        return damage
    }

    /** Domain Expansion: from Tokyo with at least 3 final Claw faces, defenses are ignored (Infinity too). */
    fun bypassesDefenses(rules: RulesConfig, attacker: PlayerState, claws: Int, inTokyo: Boolean): Boolean =
        attacker.has(CardId.DOMAIN_EXPANSION) && inTokyo && claws >= rules.domainExpansionBypassClaws
}
