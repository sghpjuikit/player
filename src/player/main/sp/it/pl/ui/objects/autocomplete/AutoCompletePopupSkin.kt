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

import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.Skin
import javafx.scene.control.cell.TextFieldListCell
import javafx.scene.input.KeyCode.DOWN
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.KeyCode.KP_DOWN
import javafx.scene.input.KeyCode.KP_UP
import javafx.scene.input.KeyCode.PAGE_DOWN
import javafx.scene.input.KeyCode.PAGE_UP
import javafx.scene.input.KeyCode.UP
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.util.Callback
import sp.it.util.functional.invoke
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sizes
import sp.it.util.reactive.sync
import sp.it.util.reactive.zip
import sp.it.util.type.nullify
import sp.it.util.ui.listView

open class AutoCompletePopupSkin<T>: Skin<AutoCompletePopup<T>> {
   private val control: AutoCompletePopup<T>
   private val list: ListView<T>
   private val disposer = Disposer()

   constructor(skinnable: AutoCompletePopup<T>, activationClickCount: Int = 1) {
      this.control = skinnable
      this.list = listView {
         cellFactory = Callback { buildListCell(it) }
         onEventDown(MOUSE_CLICKED) { if (it.button==PRIMARY && it.clickCount==activationClickCount) { chooseSuggestion(); it.consume() } }
         isFocusTraversable = false
      }
   }

   override fun getNode() = list

   override fun getSkinnable() = control

   override fun install() {
      super.install()

      list.apply {
         items = control.suggestions

         cellFactory = Callback { buildListCell(it) }
         control.visibleRowCount zip items.sizes() zip fixedCellSizeProperty() sync { (counts, cellSize) ->
            val (rowCount, itemCount) = counts
            prefHeight = snappedTopInset() + snappedBottomInset() + cellSize.toDouble()*minOf(rowCount, itemCount.toInt())
         } on disposer

      }

      val listEventKeys = setOf(UP, KP_UP, PAGE_UP, DOWN, KP_DOWN, PAGE_DOWN)
      control.onEventDown(KEY_PRESSED) { if (it.code==ENTER) { chooseSuggestion(); it.consume() } } on disposer
      control.onEventDown(KEY_PRESSED) { if (it.code==ESCAPE && control.isHideOnEscape) { control.hide(); it.consume() } } on disposer
      control.onEventDown(KEY_PRESSED) { if (it.code in listEventKeys) { list.fireEvent(it); it.consume() } } on disposer
   }

   override fun dispose() {
      disposer()
      list.items = null
      nullify(::list)
      nullify(::control)
   }

   protected open fun chooseSuggestionInput(): String = ""

   protected fun chooseSuggestion() {
      if (!list.selectionModel.isEmpty)
         control.onSuggestion(chooseSuggestionInput() to list.selectionModel.selectedItem)
   }

   protected open fun buildListCell(listView: ListView<T>): ListCell<T> = TextFieldListCell.forListView(control.converter)(listView)

}