/*
 * Impl based on ControlsFX
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

package gui.objects.rating

import de.jensd.fx.glyphs.GlyphIcons
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.STAR
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.STAR_ALT
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.CacheHint
import javafx.scene.Node
import javafx.scene.control.SkinBase
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import javafx.scene.layout.HBox
import javafx.scene.shape.Rectangle
import util.Util.clip
import util.graphics.Icons
import util.graphics.pseudoclass
import java.lang.Math.ceil

/** Skin for [Rating]. */
class RatingSkin(r: Rating) : SkinBase<Rating>(r) {

    private val backgroundContainer = HBox()
    private lateinit var backgroundIcons: Node
    private val foregroundContainer = HBox()
    private lateinit var foregroundIcons: Node
    private val foregroundMask = Rectangle()
    private var ratingOld = r.rating.get()

    init {
        backgroundContainer.alignment = Pos.CENTER
        backgroundContainer.onMouseMoved = EventHandler<MouseEvent> {
            if (skinnable.editable.get()) {
                val v = computeRating(it.sceneX, it.sceneY)
                updateClip(v)
                it.consume()
            }
        }
        backgroundContainer.onMouseClicked = EventHandler<MouseEvent> {
            if (skinnable.editable.get() && it.button == PRIMARY) {
                val v = computeRating(it.sceneX, it.sceneY)
                updateClip(v)
                ratingOld = v
                skinnable.onRatingEdited.accept(v)
                it.consume()
            }
        }
        foregroundContainer.alignment = Pos.CENTER
        foregroundContainer.isMouseTransparent = true
        foregroundContainer.clip = foregroundMask
        children.setAll(backgroundContainer, foregroundContainer)
        recreateButtons()

        registerChangeListener(r.rating) { updateClip() }
        registerChangeListener(r.icons) { recreateButtons() }
        registerChangeListener(r.partialRating) { updateClip() }

        r.addEventHandler(MOUSE_ENTERED) {
            it.consume()
            if (r.editable.get()) ratingOld = r.rating.get()
        }
        r.addEventHandler(MOUSE_EXITED) {
            it.consume()
            if (r.editable.get()) updateClip(ratingOld)
        }
    }

    private fun recreateButtons() {
        fun createButton(icon: GlyphIcons) = Icons.createIcon(icon, skinnable.icons.get(), 10).apply {
            isCache = true
            cacheHint = CacheHint.SPEED
            styleClass.setAll("rating-button")
            isMouseTransparent = true
        }

        backgroundIcons = createButton(STAR_ALT)
        foregroundIcons = createButton(STAR).apply {
            styleClass += SELECTED
        }
        backgroundContainer.children += backgroundIcons
        foregroundContainer.children += foregroundIcons

        updateClip()
    }

    private fun computeRating(sceneX: Double, sceneY: Double): Double {
        val b = backgroundIcons.sceneToLocal(sceneX, sceneY)
        val w = backgroundIcons.layoutBounds.width
        val gap = 2.0
        val x = when {
                -gap>b.x -> ratingOld
                b.x>w+gap -> ratingOld
                else -> clip(0.0, b.x/w, 1.0)
            }

        return if (skinnable.partialRating.get()) {
            x
        } else {
            val icons = skinnable.icons.get().toDouble()
            return ceil(x * icons) / icons
        }
    }

    private fun updateClip(v: Double = skinnable.rating.get()) {
        val icons = foregroundIcons.boundsInParent
        foregroundMask.width = icons.minX + v*icons.width
        foregroundMask.height = skinnable.height

        val is1 = v == 1.0
        foregroundContainer.children.forEach { it.pseudoClassStateChanged(max, is1) }
        val is0 = v == 0.0
        backgroundContainer.children.forEach { it.pseudoClassStateChanged(min, is0) }
    }

    override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
        super.layoutChildren(x, y, w, h)
        updateClip()
    }

    companion object {
        val SELECTED = "strong"
        val max = pseudoclass("max")
        val min = pseudoclass("min")
    }
}