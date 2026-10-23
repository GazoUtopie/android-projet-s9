package fr.epita.thecullinggames.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import fr.epita.thecullinggames.R

/** Wickedness gauge from 0 to 10, with the 3 / 6 / 10 thresholds highlighted. */
class WickednessGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var value = 0
        set(v) { field = v.coerceIn(0, MAX); contentDescription = "$field / $MAX"; invalidate() }

    private val density = resources.displayMetrics.density
    private val cell = RectF()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2 * density }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 11 * density
        isFakeBoldText = true
    }

    private fun color(id: Int) = ContextCompat.getColor(context, id)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (34 * density).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val gap = 3 * density
        val cellWidth = (width - gap * (MAX - 1)) / MAX
        val cellHeight = 22 * density
        for (i in 1..MAX) {
            val left = (i - 1) * (cellWidth + gap)
            cell.set(left, 0f, left + cellWidth, cellHeight)
            val threshold = i in THRESHOLDS
            fill.color = if (i <= value) color(R.color.wickedness_red) else color(R.color.cursed_surface_high)
            canvas.drawRoundRect(cell, 4 * density, 4 * density, fill)
            if (threshold) {
                stroke.color = color(R.color.points_gold)
                canvas.drawRoundRect(cell, 4 * density, 4 * density, stroke)
            }
            label.color = if (threshold) color(R.color.points_gold) else color(R.color.text_secondary)
            if (threshold) canvas.drawText(i.toString(), cell.centerX(), height - 1 * density, label)
        }
    }

    private companion object {
        const val MAX = 10
        val THRESHOLDS = setOf(3, 6, 10)
    }
}
