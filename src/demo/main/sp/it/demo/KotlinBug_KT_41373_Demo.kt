package sp.it.demo

import javafx.application.Application
import javafx.application.Platform.runLater
import javafx.scene.Scene
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.stage.Stage
import sp.it.util.dev.printIt

/** Showcasing Kotlin bug https://youtrack.jetbrains.com/issue/KT-41373 */
class KotlinBugKt41373Demo: Application() {
   override fun start(stage: Stage) {
      val header = "After window is shown, exception should be raised and stacktrace displayed in this area\n\n"
      val area = TextArea().apply {
         prefWidth = 900.0
         prefHeight = 900.0
      }
      val root = VBox(area)
      val scene = Scene(root)

      stage.scene = scene
      stage.show()

      runLater {
         runCatching {
            TextField().apply {
               root.children += this
               applyCss() // initializes skin
               skin.printIt() // non null
               skin::class.printIt() // subclass of TextInputControl.class, which defines the unresolved class
               inputMethodRequests.printIt() // non null
               inputMethodRequests::class.printIt() // anonymous class extending ExtendedInputMethodRequests
               inputMethodRequests::class.objectInstance.printIt() //
            }
         }.onFailure {
            area.text = header + it.stackTraceToString()
         }
      }
   }
}

fun main(args: Array<String>) {
   Application.launch(JavaFxBugGetScreensEmpty::class.java, *args)
}