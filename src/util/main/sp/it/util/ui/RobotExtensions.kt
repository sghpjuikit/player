package sp.it.util.ui

import javafx.scene.input.Clipboard
import javafx.scene.input.DataFormat
import javafx.scene.input.DataFormat.PLAIN_TEXT
import javafx.scene.input.KeyCode
import javafx.scene.robot.Robot
import sp.it.util.text.chars32
import sp.it.util.ui.drag.set

fun Robot.pressReleaseShortcut(vararg keys: KeyCode) {
   keys.forEach { keyPress(it) }
   keys.reversed().forEach { keyRelease(it) }
}

fun Robot.pressReleaseText(text: String) {
   Clipboard.getSystemClipboard().set(PLAIN_TEXT, text)
   pressReleaseShortcut(KeyCode.CONTROL, KeyCode.V)
}