
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
import javafx.geometry.Pos.BOTTOM_RIGHT
import javafx.scene.Node
import javafx.scene.input.Clipboard
import javafx.scene.input.DataFormat.PLAIN_TEXT
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.attach
import sp.it.util.ui.drag.set
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.stackPane

fun FencedCodeBlock.toNode(): Node {
   val text = getContentChars().normalizeEOL()

   val vbox = stackPane {
      styleClass += "markdown-codeblock-box"
      lay += label(text) {
         styleClass += "markdown-codeblock"
      }
   }

   val s = Subscribed {
      val ip = stackPane {
         isPickOnBounds = true
         styleClass += "markdown-codeblock-box-icon"
         lay(BOTTOM_RIGHT) += Icon(FontAwesomeIcon.COPY, 35.0).onClickDo { Clipboard.getSystemClipboard()[PLAIN_TEXT] = text }
      }
      vbox.lay += ip
      Subscription { vbox.lay -= ip }
   }
   vbox.hoverProperty() attach s::subscribe

   return vbox
}