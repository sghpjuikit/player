
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

package sp.it.pl.ui.objects

import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.layout.VBox
import sp.it.util.access.v
import sp.it.util.reactive.onChangeAndNow

open class MdNodeContent: VBox() {
   val mdString = v("")

   init {
      mdString.onChangeAndNow { updateContent() }
   }

   protected fun updateContent() {
      val content = MdNodeHelper(this, mdString.value)
      children.clear()
      children += content
   }

   open fun showChapter(currentChapter: IntArray?) = true

   open fun setLink(node: Node, link: String, description: String) {}

   open fun generateImage(url: String): Node = Group()

}