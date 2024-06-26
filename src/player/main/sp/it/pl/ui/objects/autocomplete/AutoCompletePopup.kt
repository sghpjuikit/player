/*
 * Implementation based on ControlsF:
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

package sp.it.pl.ui.objects.autocomplete

import com.sun.javafx.scene.control.Properties.COMBO_BOX_STYLE_CLASS
import javafx.collections.FXCollections.observableArrayList
import javafx.scene.Node
import javafx.scene.control.PopupControl
import javafx.util.StringConverter
import sp.it.util.access.v
import sp.it.util.dev.fail
import sp.it.util.dev.printIt
import sp.it.util.reactive.Handler1

/** Provides a list of available suggestions in order to complete current user input. */
open class AutoCompletePopup<T>: PopupControl() {
   /** Presented suggestions. */
   val suggestions = observableArrayList<T>()!!
   /** The converter used to turn a suggestion into a string. */
   var converter: StringConverter<T>? = null
   /** The maximum number of rows to be visible in the popup. Affects the height of the popup. Default 10. */
   val visibleRowCount = v(10)
   /** Suggestion handlers */
   val onSuggestion = Handler1<Pair<String, T>>()

   init {
      isAutoFix = true
      isAutoHide = true
      isHideOnEscape = true
      styleClass += COMBO_BOX_STYLE_CLASS
      styleClass += STYLE_CLASS
   }

   /** Shows this popup right below the specified node. Node must be attached to scene graph in a window. */
   fun show(node: Node) {
      if (isShowing) return

      val scene = node.scene ?: fail { "Can not show popup. The node must be attached to a scene." }
      val window = scene.window ?: fail { "Can not show popup. The node must be attached to a window." }
      val pos = node.localToScreen(0.0, node.boundsInLocal.getMaxY());
      show(window, pos.x, pos.y)
   }

   override fun createDefaultSkin() = AutoCompletePopupSkin(this, 1)

   companion object {
      const val STYLE_CLASS = "auto-complete-popup"
   }

}