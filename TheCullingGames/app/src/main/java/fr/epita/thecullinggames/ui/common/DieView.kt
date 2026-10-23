package fr.epita.thecullinggames.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import fr.epita.thecullinggames.R
import fr.epita.thecullinggames.business.model.DieFace

/**
 * Custom die: a rounded square showing the face symbol.
 * A kept die gets a purple glow, the Cybercerveau die a "×2" badge, an unrolled die a "?".
 */
class DieView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var face: DieFace? = null
        set(value) { field = value; updateDescription(); invalidate() }
    var kept = false
        set(value) { field = value; updateDescription(); invalidate() }
    var doubled = false
        set(value) { field = value; invalidate() }

    private val density = resources.displayMetrics.density
    private val rect = RectF()
    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.wickedness_red) }
    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        color = ContextCompat.getColor(context, R.color.white)
    }

    private fun color(id: Int) = ContextCompat.getColor(context, id)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Always square, sized by the available width.
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val size = if (width > 0) width else (48 * density).toInt()
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        val inset = 4 * density
        rect.set(inset, inset, width - inset, height - inset)
        val radius = width * 0.18f

        facePaint.color = when {
            face == null -> color(R.color.cursed_surface_high)
            kept -> color(R.color.cursed_purple)
            else -> color(R.color.die_face)
        }
        canvas.drawRoundRect(rect, radius, radius, facePaint)

        borderPaint.strokeWidth = (if (kept) 3 else 1) * density
        borderPaint.color = if (kept) color(R.color.points_gold) else color(R.color.cursed_outline)
        canvas.drawRoundRect(rect, radius, radius, borderPaint)

        val current = face
        textPaint.textSize = height * 0.45f
        textPaint.color = when (current) {
            null -> color(R.color.text_secondary)
            DieFace.HEART -> color(R.color.wickedness_red)
            DieFace.ENERGY -> color(R.color.cursed_purple_dark)
            DieFace.CLAW -> color(R.color.black)
            else -> color(R.color.die_text)
        }
        val text = current?.let { Visuals.symbol(it) } ?: "?"
        val y = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(text, rect.centerX(), y, textPaint)

        if (doubled) {
            val r = width * 0.17f
            val cx = rect.right - r * 0.6f
            val cy = rect.top + r * 0.6f
            canvas.drawCircle(cx, cy, r, badgePaint)
            badgeTextPaint.textSize = r * 1.0f
            canvas.drawText("×2", cx, cy - (badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2, badgeTextPaint)
        }
    }

    private fun updateDescription() {
        val name = face?.let { context.getString(Visuals.faceName(it)) } ?: "?"
        contentDescription = if (kept) "$name, ${context.getString(R.string.die_kept)}" else name
    }
}
