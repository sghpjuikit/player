package sp.it.pl.plugin.impl

import javafx.scene.input.KeyCode
import javafx.scene.robot.Robot

// Invoke javafx Robot to invoke ALT+F4
fun invokeAltF4() {
   Robot().apply {
      keyPress(KeyCode.ALT)
      keyPress(KeyCode.F4)
      keyRelease(KeyCode.F4)
      keyRelease(KeyCode.ALT)
   }
}