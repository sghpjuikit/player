package sp.it.pl.gui.pane

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.Priority.NEVER
import javafx.scene.layout.StackPane
import sp.it.pl.gui.objects.icon.CheckIcon
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.main.AppErrors
import sp.it.pl.main.IconFA
import sp.it.util.access.not
import sp.it.util.access.v
import sp.it.util.async.runLater
import sp.it.util.dev.stacktraceAsString
import sp.it.util.functional.runTry
import sp.it.util.functional.supplyIf
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.lookupChildAt
import sp.it.util.ui.lookupSiblingUp
import sp.it.util.ui.scrollPane
import sp.it.util.ui.textField
import sp.it.util.ui.vBox
import java.lang.management.ManagementFactory

class VmOptionsPane: StackPane() {
   init {
      val management = ManagementFactory.getPlatformMXBean(com.sun.management.HotSpotDiagnosticMXBean::class.java)
      val blowUp = v(false)
      val blowUpNot = blowUp.not()

      lay += vBox {
         lay += vBox(5.0, Pos.CENTER_LEFT) {
            styleClass += "form-config-pane"

            lay += label("I want to blow up things").scName()
            lay += label("These values have been carefully set by the developers.").scDesc()
            lay += label("Changes may have adverse effect on this program or the system.").scDesc()
            lay += label("Changes will only apply until application closes.").scDesc()
            lay += CheckIcon(blowUp)
            lay += label()
         }
         lay += label()
         lay(NEVER) += scrollPane {
            isFitToWidth = true

            content = vBox(5.0, Pos.CENTER_LEFT) {
               styleClass += "form-config-pane"

               management.diagnosticOptions.forEach { option ->
                  lay += label(option.name).scName()
                  lay += label().scDesc()
                  lay += hBox {
                     disableProperty() syncFrom blowUpNot
                     padding = Insets(0.0, 0.0, 20.0, 0.0)

                     lateinit var getter: () -> String
                     fun update() = runLater {
                        val optionRefreshed = management.getVMOption(option.name)
                        lookupSiblingUp<Label>(1).text = "Origin: ${optionRefreshed.origin.name}"
                        lookupChildAt<TextField>(0).text = optionRefreshed.value
                     }

                     lay += textField {
                        isEditable = option.isWriteable
                        getter = ::getText
                     }
                     lay += supplyIf(option.isWriteable) {
                        Icon(IconFA.PLAY).apply {
                           isDisable = !option.isWriteable
                           tooltip("Apply value")
                           onClickDo {
                              runTry {
                                 management.setVMOption(option.name, getter())
                              }.ifError {
                                 AppErrors.push("Failed to set vm option ${option.name}, because: ${it.message}", "Reason:\n${it.stacktraceAsString}")
                              }
                              update()
                           }
                        }
                     }
                     update()
                  }
               }
            }
         }
      }
   }

   companion object {
      fun Label.scName() = apply { styleClass += "form-config-pane-config-name" }
      fun Label.scDesc() = apply { styleClass += "form-config-pane-config-description" }
   }
}