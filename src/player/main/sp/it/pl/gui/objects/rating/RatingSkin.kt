package sp.it.pl.gui.objects.rating

import de.jensd.fx.glyphs.GlyphIcons
import javafx.scene.CacheHint
import javafx.scene.Node
import javafx.scene.control.SkinBase
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import javafx.scene.input.MouseEvent.MOUSE_MOVED
import javafx.scene.layout.HBox
import javafx.scene.shape.Rectangle
import sp.it.pl.main.IconFA
import sp.it.util.Util.clip
import sp.it.util.collections.setTo
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.syncTo
import sp.it.util.reactive.syncWhile
import sp.it.util.ui.createIcon
import sp.it.util.ui.hBox
import sp.it.util.ui.pseudoclass
import java.lang.Math.ceil
import kotlin.math.roundToInt

/** Skin for [Rating]. */
class RatingSkin(r: Rating): SkinBase<Rating>(r) {

    private val backgroundContainer = hBox()
    private val foregroundContainer = object: HBox() {
        override fun requestLayout() {
            if (::foregroundIcons.isInitialized) updateClip()
            super.requestLayout()
        }
    }
    private lateinit var backgroundIcons: Node
    private lateinit var foregroundIcons: Node
    private val foregroundMask = Rectangle()
    private var ratingOld = r.rating.value
    private val onDispose = Disposer()

    init {
        foregroundContainer.isMouseTransparent = true
        foregroundContainer.clip = foregroundMask
        children setTo listOf(backgroundContainer, foregroundContainer)

        r.alignment syncTo backgroundContainer.alignmentProperty() on onDispose
        r.alignment syncTo foregroundContainer.alignmentProperty() on onDispose
        r.rating attach { updateClipAndStyle() } on onDispose
        r.icons attach { updateButtons() } on onDispose
        r.partialRating attach { updateClipAndStyle() } on onDispose
        r.editable syncWhile {
            backgroundContainer.onEventDown(MOUSE_MOVED) {
                val v = computeRating(it.sceneX, it.sceneY)
                updateClipAndStyle(v)
            }
            backgroundContainer.onEventDown(MOUSE_CLICKED, PRIMARY, false) {
                val v = computeRating(it.sceneX, it.sceneY)
                updateClipAndStyle(v)
                ratingOld = v
                skinnable.onRatingEdited(v)
                it.consume()
            }
            backgroundContainer.onEventDown(MOUSE_ENTERED) { ratingOld = r.rating.value }
            backgroundContainer.onEventDown(MOUSE_EXITED) { updateClipAndStyle(ratingOld) }
        } on onDispose

        updateButtons()
    }

    private fun updateButtons() {
        fun createButton(icon: GlyphIcons) = createIcon(icon, skinnable.icons.value, 8.0).apply {
            isCache = true
            cacheHint = CacheHint.SPEED
            styleClass += "rating-button"
            isMouseTransparent = true
        }

        backgroundIcons = createButton(IconFA.STAR_ALT)
        foregroundIcons = createButton(IconFA.STAR).apply {
            styleClass += selected
        }
        backgroundContainer.children += backgroundIcons
        foregroundContainer.children += foregroundIcons

        updateClipAndStyle()
    }

    private fun computeRating(sceneX: Double, sceneY: Double): Double {
        val b = backgroundIcons.sceneToLocal(sceneX, sceneY)
        val w = backgroundIcons.layoutBounds.width
        val gap = 2.0
        val x = when {
            -gap>b.x -> ratingOld ?: 0.0
            b.x>w+gap -> ratingOld ?: 0.0
            else -> clip(0.0, b.x/w, 1.0)
        }

        return if (skinnable.partialRating.value) {
            x
        } else {
            val icons = skinnable.icons.value.toDouble()
            ceil(x*icons)/icons
        }
    }

    private fun updateClip(v: Double? = skinnable.rating.value) {
        val icons = foregroundIcons.boundsInParent
        foregroundMask.width = icons.minX.roundToInt()-1.0+(v ?: 0.0)*(icons.width+1.0)
        foregroundMask.height = skinnable.height
    }

    private fun updateStyle(v: Double? = skinnable.rating.value) {
        val isEmpty = v==null
        backgroundContainer.children.forEach { it.pseudoClassStateChanged(empty, isEmpty) }
        val is0 = v==0.0
        backgroundContainer.children.forEach { it.pseudoClassStateChanged(min, is0) }
        val is1 = v==1.0
        foregroundContainer.children.forEach { it.pseudoClassStateChanged(max, is1) }
    }

    private fun updateClipAndStyle(v: Double? = skinnable.rating.value) {
        updateClip(v)
        updateStyle(v)
    }

    override fun layoutChildren(contentX: Double, contentY: Double, contentWidth: Double, contentHeight: Double) {
        super.layoutChildren(contentX, contentY, contentWidth, contentHeight)
        if (::foregroundIcons.isInitialized) updateClip()
    }

    override fun dispose() {
        onDispose()
        super.dispose()
    }

    companion object {
        const val selected = "strong"
        val empty = pseudoclass("empty")
        val min = pseudoclass("min")
        val max = pseudoclass("max")
    }
}