package sp.it.pl.main

import sp.it.pl.ui.pane.action
import sp.it.util.type.isObject

/** Denotes actions for [Any] */
object AppActionsAny {

   val detectContent = action<Any>("Detect content", "Identifies the type of the specified content and shows appropriate ui for it", IconMD.MAGNIFY, constriction = { it !is App && !it::class.isObject }) {
      it.detectContent()
   }

}