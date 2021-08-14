package sp.it.demo

import javafx.application.Application
import javafx.scene.paint.Color.BLACK
import javafx.scene.paint.Color.TRANSPARENT
import javafx.scene.paint.CycleMethod.NO_CYCLE
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop
import javafx.scene.shape.Rectangle
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import sp.it.util.ui.bgr
import sp.it.util.ui.lay
import sp.it.util.ui.scene
import sp.it.util.ui.size
import sp.it.util.ui.stackPane
import sp.it.util.ui.stage
import sp.it.util.ui.x

/**
 * Demo for clips with alpha channel.
 */
class GradientClipDemo: Application() {

   override fun start(stage: Stage) {
      val w = Screen.getPrimary().bounds.width

      stage(StageStyle.TRANSPARENT) {
         size = 500 x w
         scene = scene(
            stackPane {
               background = bgr(TRANSPARENT)

               lay += Rectangle(500.0, w, BLACK).apply {
                  clip = Rectangle(500.0, w).apply {
                     fill = LinearGradient(0.0, 0.0, 400.0, 0.0, false, NO_CYCLE, Stop(1.0, BLACK), Stop(0.0, TRANSPARENT))
                  }
               }
            }
         ) {
            fill = TRANSPARENT
         }

         show()
      }
   }

   companion object {

      @JvmStatic fun main(args: Array<String>) {
         launch(*args)
      }
   }

}