/*
 * Implementation based on ControlsFX
 *
 * Copyright (c) 2013, 2015, ControlsFX
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

package sp.it.pl.ui.objects.textfield

import javafx.scene.Node
import javafx.scene.control.TextField
import javafx.scene.input.ContextMenuEvent
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import sp.it.pl.ui.showContextMenu
import sp.it.util.access.editable
import sp.it.util.collections.observableList
import sp.it.util.functional.ifNotNull
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.ui.pseudoClassChanged

/** [TextField], which can be decorated with nodes inside on the left and right. */
open class SpitTextField: TextField() {
   /** Content on the left. Default empty. */
   val left = observableList<Node>()
   /** Content on the right. Default empty.  */
   val right = observableList<Node>()
   /** Context menu or null if falling back on [TextField] context menu behavior. Default null. */
   var contextMenuShower: ((MouseEvent) -> Unit)? = null

   init {
      styleClass += STYLECLASS
      minWidth = USE_PREF_SIZE
      maxWidth = USE_PREF_SIZE

      // custom context menu
      editable sync { pseudoClassChanged("editable", it) }

      // custom context menu
      contextMenuShower = { showContextMenu(this, it, this::getText, null) }
      onEventDown(ContextMenuEvent.ANY) {
         if (contextMenuShower!=null)
            it.consume()
      }
      onEventDown(MOUSE_CLICKED, SECONDARY, false) {
         contextMenuShower.ifNotNull { show ->
            show(it)
            it.consume()
         }
      }
   }

   override fun createDefaultSkin() = SpitTextFieldSkin(this)

   companion object {
      const val STYLECLASS = "decorated-text-field"
   }

}