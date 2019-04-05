/*
 * Implementation based on ControlsFX
 *
 * Copyright (c) 2014, 2015, ControlsFX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
import sp.it.pl.util.Util.clip
import sp.it.pl.util.collections.setTo
import sp.it.pl.util.reactive.Disposer
import sp.it.pl.util.reactive.attach
import sp.it.pl.util.reactive.on
import sp.it.pl.util.reactive.onEventDown
import sp.it.pl.util.reactive.syncFrom
import sp.it.pl.util.ui.createIcon
import sp.it.pl.util.ui.hBox
import sp.it.pl.util.ui.pseudoclass
import java.lang.Math.ceil
import kotlin.math.roundToInt

/** Skin for [Rating]. */
class RatingSkin(r: Rating): SkinBase<Rating>(r) {

    private val backgroundContainer = hBox()
    private lateinit var backgroundIcons: Node
    private val foregroundContainer = object: HBox() {
        override fun requestLayout() {
            if (::foregroundIcons.isInitialized) updateClip()
            super.requestLayout()
        }
    }
    private lateinit var foregroundIcons: Node
    private val foregroundMask = Rectangle()
    private var ratingOld = r.rating.value
    private val onDispose = Disposer()

    init {
        backgroundContainer.alignmentProperty() syncFrom r.alignment on onDispose
        backgroundContainer.onEventDown(MOUSE_MOVED) {
            if (skinnable.editable.value) {
                val v = computeRating(it.sceneX, it.sceneY)
                updateClipAndStyle(v)
            }
        }
        backgroundContainer.onEventDown(MOUSE_CLICKED, PRIMARY, false) {
            if (skinnable.editable.value) {
                val v = computeRating(it.sceneX, it.sceneY)
                updateClipAndStyle(v)
                ratingOld = v
                skinnable.onRatingEdited(v)
                it.consume()
            }
        }

        foregroundContainer.alignmentProperty() syncFrom r.alignment on onDispose
        foregroundContainer.isMouseTransparent = true
        foregroundContainer.clip = foregroundMask
        children setTo listOf(backgroundContainer, foregroundContainer)
        updateButtons()

        r.rating attach { updateClipAndStyle() } on onDispose
        r.icons attach { updateButtons() } on onDispose
        r.partialRating attach { updateClipAndStyle() } on onDispose

        r.onEventDown(MOUSE_ENTERED) {
            if (r.editable.value) ratingOld = r.rating.value
        }
        r.onEventDown(MOUSE_EXITED) {
            if (r.editable.value) updateClipAndStyle(ratingOld)
        }
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