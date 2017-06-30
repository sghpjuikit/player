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
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import javafx.scene.layout.HBox
import javafx.scene.shape.Rectangle
import util.Util.clip
import util.graphics.Icons
import util.graphics.css.pseudoClass
import java.lang.Math.ceil

/** Skin for [Rating]. */
class RatingSkin(r: Rating) : SkinBase<Rating>(r) {

    private var backgroundContainer = HBox()
    private var foregroundContainer = HBox()
    private var foregroundMask = Rectangle()
    private var old_rating = 0.0
    private val mouseMoveHandler = EventHandler<MouseEvent> {
        if (!skinnable.updateOnHover.get() || !skinnable.editable.get())
            return@EventHandler

        val v = calculateRating(it.sceneX, it.sceneY)
        updateClip(v)

        it.consume()
    }
    private val mouseClickHandler = EventHandler<MouseEvent> {
        if (!skinnable.editable.get() || it.button == SECONDARY)
            return@EventHandler

        val v = calculateRating(it.sceneX, it.sceneY)
        updateClip(v)
        old_rating = v

        skinnable.onRatingByUserChanged.accept(v)

        it.consume()
    }

    init {
        recreateButtons()

        registerChangeListener(r.rating) { updateClip(r.rating.get()) }
        registerChangeListener(r.icons) { recreateButtons() }
        registerChangeListener(r.updateOnHover) { updateClip(r.rating.get()) }
        registerChangeListener(r.partialRating) { updateClip(r.rating.get()) }

        // remember rating and return to old after mouse hover ends
        r.addEventHandler(MOUSE_ENTERED) { e ->
            e.consume()
            if (r.updateOnHover.get())
                old_rating = r.rating.get()
        }
        r.addEventHandler(MOUSE_EXITED) { e ->
            e.consume()
            if (r.updateOnHover.get())
                updateClip(old_rating)
        }
    }

    private fun recreateButtons() {
        backgroundContainer = HBox()
        backgroundContainer.alignment = Pos.CENTER
        foregroundContainer = HBox()
        foregroundContainer.alignment = Pos.CENTER
        foregroundContainer.isMouseTransparent = true
        children.setAll(backgroundContainer, foregroundContainer)

        foregroundMask = Rectangle()
        foregroundContainer.clip = foregroundMask
        val b = createButton(STAR_ALT)
        val f = createButton(STAR)
        f.styleClass += SELECTED
        f.isMouseTransparent = true
        foregroundContainer.children += f
        backgroundContainer.children += b

        updateClip(skinnable.rating.get())
    }

    // returns rating based on scene relative mouse position
    private fun calculateRating(sceneX: Double, sceneY: Double): Double {
        // get 0-1 position value
        val r = skinnable
        val b = backgroundContainer.sceneToLocal(sceneX, sceneY)
        val leftP = backgroundContainer.snappedLeftInset()
        val rightP = backgroundContainer.snappedRightInset()
        val w = r.width - leftP - rightP
        var x = b.x - leftP
        x = clip(0.0, x, w)
        // make 2px space for min & max value
        val extra = 2 / w
        x = x * (1 + 2 * extra) - extra
        // calculate the rating value
        var nv = clip(0.0, x / w, 1.0)
        // ceil to int if needed
        val icons = r.icons.get().toDouble()
        if (!r.partialRating.get()) nv = ceil(nv * icons) / icons

        return nv
    }

    // updates the skin to the current values
    private fun updateClip(v: Double) {
        val r = skinnable
        val w = r.width - (backgroundContainer.snappedLeftInset() + backgroundContainer.snappedRightInset())
        val x = w * v

        foregroundMask.width = x
        foregroundMask.height = r.height

        val is1 = v == 1.0
        foregroundContainer.children.forEach { it.pseudoClassStateChanged(max, is1) }
        val is0 = v == 0.0
        backgroundContainer.children.forEach { it.pseudoClassStateChanged(min, is0) }
    }

    private fun createButton(icon: GlyphIcons): Node {
        val l = Icons.createIcon(icon, skinnable.icons.get(), 10)
        l.isCache = true
        l.cacheHint = CacheHint.SPEED
        l.styleClass.setAll("rating-button")
        l.onMouseMoved = mouseMoveHandler
        l.onMouseClicked = mouseClickHandler
        return l
    }

    override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
        super.layoutChildren(x, y, w, h)
        updateClip(skinnable.rating.get())
    }

    companion object {
        val SELECTED = "strong"
        val max = pseudoClass("max")
        val min = pseudoClass("min")
    }
}