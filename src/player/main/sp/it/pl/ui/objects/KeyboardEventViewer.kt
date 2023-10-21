/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package sp.it.pl.ui.objects

import javafx.event.EventHandler
import javafx.geometry.Side.RIGHT
import javafx.scene.input.InputMethodEvent
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import sp.it.pl.main.IconOC
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.reactive.onEventDown
import sp.it.util.ui.textArea

/** Keyboard / InputMethod Event Viewer */
class KeyboardEventViewer: BorderPane() {
   private val textArea = textArea {
      isEditable = false
      onEventDown(KeyEvent.ANY) { handle(it) }
      onInputMethodTextChanged = EventHandler { handle(it) }
   }

   init {
      top = Icon(IconOC.TRASHCAN).onClickDo { clear() }.withText(RIGHT, "Clear")
      center = textArea
   }

   override fun requestFocus() = textArea.requestFocus()

   private fun clear() = textArea.clear()

   private fun append(text: StringBuilder.() -> Unit) {
      textArea.appendText(StringBuilder().apply(text).toString())
      textArea.scrollTop = Double.MAX_VALUE
   }

   private fun handle(e: KeyEvent) {
      append {
         append("KeyEvent{")
         append("type=").append(e.eventType)
         append(", character=").append(e.character)
         append(", text=").append(e.text)
         append(", code=").append(e.code)
         if (e.isShiftDown) append(", shift")
         if (e.isControlDown) append(", control")
         if (e.isAltDown) append(", alt")
         if (e.isMetaDown) append(", meta")
         if (e.isShortcutDown) append(", shortcut")
         append("}\n")
      }
   }

   private fun handle(e: InputMethodEvent) {
      append {
         append("InputMethodEvent{")
         append("type=").append(e.eventType)
         append(", caret=").append(e.caretPosition)
         if (e.committed.isNotEmpty()) {
            append(", committed=")
            append(e.committed)
         }
         if (!e.composed.isEmpty()) {
            append(", composed=")
            for (run in e.composed)
               append(run.text)
         }
         append("}\n")
      }
   }
}