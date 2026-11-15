package fr.epita.thecullinggames.ui.game

import android.animation.ValueAnimator
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import fr.epita.thecullinggames.R
import fr.epita.thecullinggames.databinding.GameFeedbackOverlayBinding
import fr.epita.thecullinggames.ui.common.EventFormatter
import fr.epita.thecullinggames.ui.common.Visuals

/**
 * Displays the [Feedback] items chosen by the ViewModel on the overlay: banner, large card or tile,
 * flight towards the players concerned, panel flashes and floating texts. Texts come from
 * [EventFormatter]: nothing here deduces game facts.
 */
class FeedbackPresenter(
    private val overlay: GameFeedbackOverlayBinding,
    private val tokyoCard: MaterialCardView,
    private val formatter: EventFormatter,
    /** Player panel in the strip (scrolled into view if possible), null when not laid out. */
    private val playerPanel: (Int) -> View?,
    private val portraitOf: (Int) -> Int,
) {
    private val context = overlay.root.context
    private val density = context.resources.displayMetrics.density
    private var shownId = -1L

    private fun color(id: Int) = ContextCompat.getColor(context, id)

    /** Shows [item]; with [animate] false (already displayed before a rotation) it is shown still. */
    fun show(item: Feedback?, animate: Boolean) {
        if (item?.id == shownId) return
        shownId = item?.id ?: -1L
        hideBanner()
        hideShowcase()
        when (item) {
            null -> Unit
            is Feedback.Tokyo -> {
                showBanner(portraitOf(item.playerId), formatter.format(item.event), animate)
                if (animate) {
                    flash(tokyoCard, color(if (item.entered) R.color.wickedness_red else R.color.points_gold))
                    playerPanel(item.playerId)?.let { flash(it, color(R.color.wickedness_red)) }
                }
            }
            is Feedback.Showcase -> if (animate) showcase(item)
            is Feedback.Impacts -> if (animate) item.impacts.forEach(::impact)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Banner
    // ---------------------------------------------------------------------------------------------

    private fun showBanner(image: Int, text: String, animate: Boolean) {
        overlay.bannerImage.setImageResource(image)
        overlay.bannerText.text = text
        overlay.banner.isVisible = true
        if (!animate) return
        overlay.banner.alpha = 0f
        overlay.banner.translationY = -24 * density
        overlay.banner.animate().alpha(1f).translationY(0f).setDuration(220).start()
    }

    private fun hideBanner() {
        overlay.banner.animate().cancel()
        overlay.banner.isVisible = false
    }

    // ---------------------------------------------------------------------------------------------
    // Showcase: card or tile shown large, then sent to the players it affected
    // ---------------------------------------------------------------------------------------------

    private fun showcase(item: Feedback.Showcase) {
        val view = overlay.showcase
        val card = item.card
        if (card != null) {
            val visual = Visuals.card(card)
            overlay.showcaseImage.setImageResource(visual.image)
            overlay.showcaseImage.isVisible = true
            overlay.showcaseTitle.setText(visual.name)
        } else {
            overlay.showcaseImage.isVisible = false
            overlay.showcaseTitle.setText(Visuals.tileName(item.tile!!))
        }
        // Who bought or chose it, then what it did: all formatted from the engine events.
        overlay.showcaseText.text = (listOf(item.event) + item.effects).joinToString("\n") { formatter.format(it) }
        view.isVisible = true
        view.alpha = 0f
        view.scaleX = 0.7f
        view.scaleY = 0.7f
        view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(250).withEndAction {
            // Flight towards the first player affected, or the owner for a Power without immediate effect.
            val targetId = item.impacts.firstOrNull()?.playerId ?: item.playerId
            val (dx, dy) = offsetTo(view, playerPanel(targetId))
            view.animate().setStartDelay(1000).setDuration(450)
                .translationX(dx).translationY(dy).scaleX(0.2f).scaleY(0.2f).alpha(0f)
                .withEndAction {
                    hideShowcase()
                    if (item.impacts.isEmpty()) {
                        playerPanel(item.playerId)?.let { flash(it, color(R.color.cursed_purple)) }
                    } else {
                        item.impacts.forEach(::impact)
                    }
                }
                .start()
        }.start()
    }

    private fun hideShowcase() {
        val view = overlay.showcase
        view.animate().cancel()
        view.isVisible = false
        view.translationX = 0f
        view.translationY = 0f
        view.scaleX = 1f
        view.scaleY = 1f
        view.alpha = 1f
    }

    /** Translation that brings the centre of [view] onto the centre of [target]. */
    private fun offsetTo(view: View, target: View?): Pair<Float, Float> {
        if (target == null) return 0f to 0f
        val from = IntArray(2).also { view.getLocationInWindow(it) }
        val to = IntArray(2).also { target.getLocationInWindow(it) }
        val dx = (to[0] + target.width / 2f) - (from[0] + view.width / 2f)
        val dy = (to[1] + target.height / 2f) - (from[1] + view.height / 2f)
        return dx to dy
    }

    // ---------------------------------------------------------------------------------------------
    // Impacts on a player: panel flash and floating text (damage, heal, Energy, points)
    // ---------------------------------------------------------------------------------------------

    private fun impact(impact: Impact) {
        val panel = playerPanel(impact.playerId) ?: return
        val (tint, text) = when (impact.kind) {
            ImpactKind.DAMAGE, ImpactKind.HP_LOSS -> R.color.wickedness_red to context.getString(R.string.impact_hp, impact.amount)
            ImpactKind.HEAL -> R.color.heal_green to context.getString(R.string.impact_hp, impact.amount)
            ImpactKind.ENERGY -> R.color.energy_cyan to context.getString(R.string.impact_energy, impact.amount)
            ImpactKind.VP -> R.color.points_gold to context.getString(R.string.impact_vp, impact.amount)
        }
        flash(panel, color(tint))
        floatingText(panel, text, color(tint))
    }

    private fun floatingText(anchor: View, text: String, tint: Int) {
        val layer = overlay.impactLayer
        val origin = IntArray(2).also { layer.getLocationInWindow(it) }
        val at = IntArray(2).also { anchor.getLocationInWindow(it) }
        val label = TextView(context).apply {
            this.text = text
            setTextColor(tint)
            textSize = 20f
            setShadowLayer(6f, 0f, 0f, color(R.color.black))
            gravity = Gravity.CENTER
        }
        val params = FrameLayout.LayoutParams(anchor.width, FrameLayout.LayoutParams.WRAP_CONTENT)
        params.leftMargin = at[0] - origin[0]
        params.topMargin = at[1] - origin[1] + anchor.height / 3
        layer.addView(label, params)
        label.animate().translationY(-40 * density).alpha(0f).setDuration(1100)
            .withEndAction { layer.removeView(label) }.start()
    }

    /** Brief coloured glow over a card, without touching its normal styling. */
    private fun flash(view: View, tint: Int) {
        val glow = ColorDrawable(tint).apply { alpha = 0 }
        view.foreground = glow
        ValueAnimator.ofInt(0, 140, 0).apply {
            duration = 900
            addUpdateListener { glow.alpha = it.animatedValue as Int }
            withEnd { if (view.foreground === glow) view.foreground = null }
        }.start()
    }

    private fun ValueAnimator.withEnd(action: () -> Unit) = addListener(object : android.animation.AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: android.animation.Animator) = action()
    })
}
