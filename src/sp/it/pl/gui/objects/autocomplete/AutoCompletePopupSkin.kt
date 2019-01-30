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

package sp.it.pl.gui.objects.autocomplete

import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.Skin
import javafx.scene.control.cell.TextFieldListCell
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.util.Callback
import sp.it.pl.util.functional.invoke
import sp.it.pl.util.graphics.listView
import sp.it.pl.util.reactive.onEventDown
import sp.it.pl.util.reactive.sizes
import sp.it.pl.util.reactive.syncTo

open class AutoCompletePopupSkin<T>: Skin<AutoCompletePopup<T>> {
    private val control: AutoCompletePopup<T>
    private val list: ListView<T>

    constructor(skinnable: AutoCompletePopup<T>, activationClickCount: Int = 1) {
        control = skinnable
        list = listView {
            items = control.suggestions

            syncTo(control.visibleRowCount, items.sizes(), fixedCellSizeProperty()) { rowCount, itemCount, cellSize ->
                prefHeight = snappedTopInset() + snappedBottomInset() + cellSize.toDouble()*minOf(rowCount, itemCount.toInt())
            }
            cellFactory = Callback { buildListViewCellFactory(it) }

            onEventDown(MOUSE_CLICKED) {
                if (it.button==MouseButton.PRIMARY && it.clickCount==activationClickCount) {
                    chooseSuggestion()
                    it.consume()
                }
            }
            onEventDown(KEY_PRESSED) {
                when (it.code) {
                    KeyCode.ENTER -> {
                        chooseSuggestion()
                        it.consume()
                    }
                    KeyCode.ESCAPE -> {
                        if (control.isHideOnEscape) {
                            control.hide()
                            it.consume()
                        }
                    }
                    else -> {
                    }
                }
            }
        }
    }

    override fun getNode() = list

    override fun getSkinnable() = control

    override fun dispose() {}

    private fun chooseSuggestion(suggestion: T? = list.selectionModel.selectedItem) {
        if (suggestion!=null)
            skinnable.onSuggestion(suggestion)
    }

    protected open fun buildListViewCellFactory(listView: ListView<T>): ListCell<T> = TextFieldListCell.forListView(control.converter)(listView)
}