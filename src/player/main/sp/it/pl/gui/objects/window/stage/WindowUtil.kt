package sp.it.pl.gui.objects.window.stage

import javafx.scene.input.MouseButton
import javafx.scene.robot.Robot
import sp.it.pl.gui.objects.picker.ContainerPicker
import sp.it.pl.gui.objects.placeholder.Placeholder
import sp.it.pl.layout.widget.initialTemplateFactory
import sp.it.pl.main.AppAnimator
import sp.it.pl.main.IconFA
import sp.it.util.async.runFX
import sp.it.util.reactive.sync1If
import sp.it.util.ui.centre
import sp.it.util.ui.toPoint2D
import sp.it.util.units.millis
import sp.it.util.units.seconds

fun Window.installStartLayoutPlaceholder() {

   fun showStartLayoutPlaceholder() {
      var action = {}
      val p = Placeholder(IconFA.FOLDER, "Start with a simple click\n\nIf you are 1st timer, choose ${ContainerPicker.choiceForTemplate} > ${initialTemplateFactory.nameGui()}") { action() }
      action = {
         runFX(300.millis) {
            AppAnimator.closeAndDo(p) {
               runFX(500.millis) {
                  p.hide()
                  Robot().apply {
                     mouseMove(root.localToScreen(root.layoutBounds).centre.toPoint2D())
                     mouseClick(MouseButton.PRIMARY)
                  }
               }
            }
         }
      }
      AppAnimator.applyAt(p, 0.0)
      p.showFor(content)
      AppAnimator.openAndDo(p) {}
   }

   s.showingProperty().sync1If({ it }) {
      runFX(1.seconds) {
         if (topContainer?.children?.isEmpty()==true) {
            showStartLayoutPlaceholder()
         }
      }
   }

}