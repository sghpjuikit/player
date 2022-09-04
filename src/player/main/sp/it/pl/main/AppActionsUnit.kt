package sp.it.pl.main

import javafx.scene.input.Clipboard
import sp.it.pl.ui.pane.action

/** Denotes actions for [Unit] */
object AppActionsUnit {

   val detectContentFromClipBoard = action<Unit>("Detect clipboard content", "Identifies the type of the clipboard content and shows appropriate ui for it", IconMD.MAGNIFY) {
      Clipboard.getSystemClipboard().getAny().detectContent()
   }

}