package fr.epita.thecullinggames.business.engine

import kotlin.random.Random

/** Injectable randomness: a seeded source makes games reproducible in tests. */
interface RandomSource {
    /** Returns a value in [0, bound). */
    fun nextInt(bound: Int): Int

    fun <T> shuffle(list: MutableList<T>) {
        for (i in list.lastIndex downTo 1) {
            val j = nextInt(i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
    }
}

class KotlinRandomSource(seed: Long? = null) : RandomSource {
    private val random = if (seed == null) Random.Default else Random(seed)
    override fun nextInt(bound: Int) = random.nextInt(bound)
}
