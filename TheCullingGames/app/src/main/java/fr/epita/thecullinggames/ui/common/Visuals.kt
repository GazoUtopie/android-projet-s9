package fr.epita.thecullinggames.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import fr.epita.thecullinggames.R
import fr.epita.thecullinggames.business.model.CardId
import fr.epita.thecullinggames.business.model.DieFace
import fr.epita.thecullinggames.business.model.TileId

/** Maps the stable business ids to Android resources (texts and images). */
object Visuals {

    data class CardVisual(@param:StringRes val name: Int, @param:StringRes val description: Int, @param:DrawableRes val image: Int)

    fun card(id: CardId): CardVisual = when (id) {
        CardId.DOMAIN_EXPANSION -> CardVisual(R.string.card_domain_expansion, R.string.card_domain_expansion_desc, R.drawable.card_domain_expansion)
        CardId.INFINITY -> CardVisual(R.string.card_infinity, R.string.card_infinity_desc, R.drawable.card_infinity)
        CardId.REVERSE_CURSED_TECHNIQUE -> CardVisual(R.string.card_reverse_cursed, R.string.card_reverse_cursed_desc, R.drawable.card_reverse_cursed)
        CardId.BINDING_VOW -> CardVisual(R.string.card_binding_vow, R.string.card_binding_vow_desc, R.drawable.card_binding_vow)
        CardId.MAHORAGA_ADAPTATION -> CardVisual(R.string.card_mahoraga, R.string.card_mahoraga_desc, R.drawable.card_mahoraga)
        CardId.SPECIAL_GRADE -> CardVisual(R.string.card_special_grade, R.string.card_special_grade_desc, R.drawable.card_special_grade)
        CardId.BLACK_FLASH -> CardVisual(R.string.card_black_flash, R.string.card_black_flash_desc, R.drawable.card_black_flash)
        CardId.VIOLET -> CardVisual(R.string.card_violet, R.string.card_violet_desc, R.drawable.card_violet)
        CardId.VEIL -> CardVisual(R.string.card_veil, R.string.card_veil_desc, R.drawable.card_veil)
        CardId.RESONANCE -> CardVisual(R.string.card_resonance, R.string.card_resonance_desc, R.drawable.card_resonance)
        CardId.NANAMI_OVERTIME -> CardVisual(R.string.card_nanami, R.string.card_nanami_desc, R.drawable.card_nanami)
        CardId.TEN_SHADOWS -> CardVisual(R.string.card_ten_shadows, R.string.card_ten_shadows_desc, R.drawable.card_ten_shadows)
        CardId.SUKUNA_FINGER -> CardVisual(R.string.card_sukuna, R.string.card_sukuna_desc, R.drawable.card_sukunas_finger)
        CardId.OVERFLOWING_ENERGY -> CardVisual(R.string.card_overflowing, R.string.card_overflowing_desc, R.drawable.card_overflowing)
        CardId.INCANTATION -> CardVisual(R.string.card_incantation, R.string.card_incantation_desc, R.drawable.card_incantation)
        CardId.DIVERGENT_FIST -> CardVisual(R.string.card_divergent_fist, R.string.card_divergent_fist_desc, R.drawable.card_divergent_fist)
        CardId.BOOGIE_WOOGIE -> CardVisual(R.string.card_boogie_woogie, R.string.card_boogie_woogie_desc, R.drawable.card_boogie_woogie)
        CardId.MAKI_GLASSES -> CardVisual(R.string.card_maki_glasses, R.string.card_maki_glasses_desc, R.drawable.card_makis_glasses)
        CardId.BLOOD_MANIPULATION -> CardVisual(R.string.card_blood_manipulation, R.string.card_blood_manipulation_desc, R.drawable.card_blood_manipulation)
        CardId.TALISMAN -> CardVisual(R.string.card_talisman, R.string.card_talisman_desc, R.drawable.card_talisman)
    }

    @StringRes
    fun tileName(id: TileId): Int = when (id) {
        TileId.INFATIGABLE -> R.string.tile_infatigable
        TileId.ETERNEL -> R.string.tile_eternel
        TileId.FURTIF -> R.string.tile_furtif
        TileId.SOURNOIS -> R.string.tile_sournois
        TileId.REGENERATION_TOTALE -> R.string.tile_regeneration
        TileId.PANIQUE_GENERALE -> R.string.tile_panique
        TileId.REPAIRE_MALEFIQUE -> R.string.tile_repaire
        TileId.CYBERCERVEAU -> R.string.tile_cyber
        TileId.RAYON_STELLAIRE -> R.string.tile_stellaire
        TileId.RAYON_ANTIMATIERE -> R.string.tile_antimatiere
    }

    @StringRes
    fun tileDescription(id: TileId): Int = when (id) {
        TileId.INFATIGABLE -> R.string.tile_infatigable_desc
        TileId.ETERNEL -> R.string.tile_eternel_desc
        TileId.FURTIF -> R.string.tile_furtif_desc
        TileId.SOURNOIS -> R.string.tile_sournois_desc
        TileId.REGENERATION_TOTALE -> R.string.tile_regeneration_desc
        TileId.PANIQUE_GENERALE -> R.string.tile_panique_desc
        TileId.REPAIRE_MALEFIQUE -> R.string.tile_repaire_desc
        TileId.CYBERCERVEAU -> R.string.tile_cyber_desc
        TileId.RAYON_STELLAIRE -> R.string.tile_stellaire_desc
        TileId.RAYON_ANTIMATIERE -> R.string.tile_antimatiere_desc
    }

    @DrawableRes
    fun portrait(characterId: String): Int = when (characterId) {
        "gojo" -> R.drawable.char_gojo
        "megumi" -> R.drawable.char_megumi
        "hakari" -> R.drawable.char_hakari
        else -> R.drawable.char_jogo
    }

    @StringRes
    fun characterTitle(characterId: String): Int = when (characterId) {
        "gojo" -> R.string.char_gojo_title
        "megumi" -> R.string.char_megumi_title
        "hakari" -> R.string.char_hakari_title
        else -> R.string.char_jogo_title
    }

    /** Short symbol drawn on a die and used in the log. */
    fun symbol(face: DieFace): String = when (face) {
        DieFace.ONE -> "1"
        DieFace.TWO -> "2"
        DieFace.THREE -> "3"
        DieFace.ENERGY -> "⚡"
        DieFace.HEART -> "♥"
        DieFace.CLAW -> "✸"
    }

    @StringRes
    fun faceName(face: DieFace): Int = when (face) {
        DieFace.ONE -> R.string.face_one
        DieFace.TWO -> R.string.face_two
        DieFace.THREE -> R.string.face_three
        DieFace.ENERGY -> R.string.face_energy
        DieFace.HEART -> R.string.face_heart
        DieFace.CLAW -> R.string.face_claw
    }
}
