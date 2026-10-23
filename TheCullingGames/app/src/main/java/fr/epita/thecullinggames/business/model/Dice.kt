package fr.epita.thecullinggames.business.model

enum class DieFace { ONE, TWO, THREE, ENERGY, HEART, CLAW;

    /** Numeric value for number faces, null otherwise. */
    val number: Int?
        get() = when (this) {
            ONE -> 1
            TWO -> 2
            THREE -> 3
            else -> null
        }
}

/** One of the six base dice. A kept die is not rerolled but can be released before a later roll. */
data class Die(val face: DieFace, val kept: Boolean = false)

/** Face groups the player resolves one by one, in the order of their choice. */
enum class ResolveGroup { NUMBERS, ENERGY, HEARTS, CLAWS }

fun DieFace.group(): ResolveGroup = when (this) {
    DieFace.ONE, DieFace.TWO, DieFace.THREE -> ResolveGroup.NUMBERS
    DieFace.ENERGY -> ResolveGroup.ENERGY
    DieFace.HEART -> ResolveGroup.HEARTS
    DieFace.CLAW -> ResolveGroup.CLAWS
}
