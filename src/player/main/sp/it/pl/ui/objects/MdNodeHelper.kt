/*
   Copied and adapted from `com.sandec.mdfx.impl.MDFXNodeHelper` from JPro-one/markdown-javafx-renderer (https://github.com/JPro-one/markdown-javafx-renderer)

   Copyright 2018 JPro-one (https://www.jpro.one) SANDEC GmbH
   Copyright 2021 spit

	   Licensed under the Apache License, Version 2.0 (the "License");
	   you may not use this file except in compliance with the License.
	   You may obtain a copy of the License at

	   http://www.apache.org/licenses/LICENSE-2.0

	   Unless required by applicable law or agreed to in writing, software
	   distributed under the License is distributed on an "AS IS" BASIS,
	   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	   See the License for the specific language governing permissions and
	   limitations under the License.
*/

@file:Suppress("UsePropertyAccessSyntax")

package sp.it.pl.ui.objects

import com.vladsch.flexmark.ast.FencedCodeBlock
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.event.EventDispatchChain
import javafx.geometry.Pos.BOTTOM_RIGHT
import javafx.scene.Node
import javafx.scene.input.Clipboard
import javafx.scene.input.DataFormat.PLAIN_TEXT
import javafx.scene.layout.StackPane
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.attach
import sp.it.util.ui.drag.set
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.stackPane
import sp.it.util.ui.text
import sp.it.util.ui.textFlow

fun FencedCodeBlock.toNode(): Node {
   val text = getContentChars().normalizeEOL().trim()

   val vbox = stackPane {
      styleClass += "markdown-codeblock-box"
      lay += textFlow {
         lay += text(text) {
            styleClass += "markdown-codeblock"
         }
      }
   }

   val s = Subscribed {
      val ip = object: StackPane() {
         init {
            isPickOnBounds = true
            styleClass += "markdown-codeblock-box-icon-box"
            lay(BOTTOM_RIGHT) += Icon(FontAwesomeIcon.COPY).onClickDo { Clipboard.getSystemClipboard()[PLAIN_TEXT] = text }
         }
         override fun computeMinHeight(width: Double) =  0.0
         override fun computeMinWidth(height: Double) = 0.0
         override fun computePrefHeight(width: Double) =  0.0
         override fun computePrefWidth(height: Double) = 0.0
      }
      vbox.lay += ip
      Subscription { vbox.lay -= ip }
   }
   vbox.hoverProperty() attach s::subscribe

   return vbox
}