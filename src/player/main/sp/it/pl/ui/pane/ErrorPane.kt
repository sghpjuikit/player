package sp.it.pl.ui.pane

import javafx.beans.value.WritableValue
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.Pos.TOP_RIGHT
import javafx.scene.control.TextArea
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.Background
import javafx.scene.layout.Border
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.Priority.NEVER
import javafx.scene.text.TextAlignment
import sp.it.pl.main.AppError
import sp.it.pl.main.AppEventLog
import sp.it.pl.main.IconFA
import sp.it.pl.main.Key
import sp.it.pl.main.emScaled
import sp.it.pl.main.toUi
import sp.it.pl.ui.objects.icon.CheckIcon
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.icon.TextIcon
import sp.it.util.access.toggleNext
import sp.it.util.access.v
import sp.it.util.async.runLater
import sp.it.util.dev.stacktraceAsString
import sp.it.util.functional.net
import sp.it.util.functional.supplyIf
import sp.it.util.math.max
import sp.it.util.math.min
import sp.it.util.reactive.attach
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.zip
import sp.it.util.ui.Util
import sp.it.util.ui.Util.computeTextHeight
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.minPrefMaxHeight
import sp.it.util.ui.setMinPrefMaxSize
import sp.it.util.ui.stackPane
import sp.it.util.ui.textAlignment
import sp.it.util.ui.vBox
import sp.it.util.units.em

class ErrorPane: OverlayPane<Any>() {

   private var uiAt = -1
   private lateinit var historyAtText: WritableValue<String>
   private val uiErrorsOnly = v(true)
   private val uiText = TextArea()
   private val uiTextScrollbarHeight = Util.getScrollBarHeightProperty(uiText)
   private var uiTextHeightUpdater = { text: String -> }

   init {
      uiErrorsOnly attach { updateIndexes() }

      uiText.apply {
         padding = Insets.EMPTY
         isEditable = false
         isWrapText = true
         textAlignment = TextAlignment.CENTER
         setMinPrefMaxSize(-1.0)
         border = Border.EMPTY
         background = Background.EMPTY
      }

      content = stackPane {
         padding = Insets(50.emScaled, 300.emScaled, 50.emScaled, 300.emScaled)
         minPrefMaxHeight = 200.0.emScaled
         onEventDown(KEY_PRESSED, Key.LEFT) { visitLeft() }
         onEventDown(KEY_PRESSED, Key.RIGHT) { visitRight() }

         lay(CENTER) += vBox {
            isFillWidth = false
            alignment = CENTER

            uiTextHeightUpdater = {
               uiText.prefHeight = (computeTextHeight(uiText.font, uiText.width, uiText.text) + 1.em.emScaled + uiTextScrollbarHeight.value) min layoutBounds.height
            }
            uiTextScrollbarHeight attach { uiTextHeightUpdater(uiText.text) }
            layoutBoundsProperty() attach { uiTextHeightUpdater(uiText.text) }

            lay += uiText.apply {
               minWidth = 400.emScaled
            }
         }
         lay(TOP_RIGHT) += vBox(0.0, CENTER_RIGHT) {
            isPickOnBounds = false
            isFillWidth = false

            lay(NEVER) += hBox(5.0, CENTER_RIGHT) {
               isPickOnBounds = false
               isFillHeight = false

               lay += Icon(IconFA.ANGLE_LEFT, -1.0, "Previous message").onClickDo { visitLeft() }
               lay += label("0/0") {
                  historyAtText = textProperty()
               }
               lay += Icon(IconFA.ANGLE_RIGHT, -1.0, "Next message").onClickDo { visitRight() }
               lay += CheckIcon(uiErrorsOnly).icons(TextIcon("Error"), TextIcon("Event"))
               lay += label("Log")
               lay += supplyIf(display.value!=Display.WINDOW) {
                  Icon(IconFA.SQUARE, -1.0, "Always on top\n\nForbid hiding this window behind other application windows").apply {
                     onClickDo {
                        stage.value?.let {
                           it.isAlwaysOnTop = !it.isAlwaysOnTop
                           icon(it.isAlwaysOnTop, IconFA.SQUARE, IconFA.SQUARE_ALT)
                        }
                     }
                  }
               }
            }
            lay(ALWAYS) += stackPane()
         }
      }
      makeResizableByUser()

      AppEventLog.history.onChange { updateIndexes() }
   }

   override fun show(data: Any) {
      uiAt = AppEventLog.history.indexOfLast { it===data }
      visit(uiAt)
      super.show()
   }

   private fun visitLeft() = visit(
      when (uiErrorsOnly.value) {
         false -> (uiAt max 1) - 1
         true -> AppEventLog.history.asSequence().withIndex().take(uiAt max 1).filter { (_, o) -> isError(o) }.lastOrNull()?.index
            ?: uiAt
      }
   )

   private fun visitRight() = visit(
      when (uiErrorsOnly.value) {
         false -> (uiAt min AppEventLog.history.size - 2) + 1
         true -> AppEventLog.history.asSequence().withIndex().drop(uiAt max 0).filter { (_, o) -> isError(o) }.take(2).lastOrNull()?.index
            ?: uiAt
      }
   )

   private fun visit(at: Int) {
      if (AppEventLog.history.isEmpty())
         update(0, null)
      else if (at in 0..AppEventLog.history.lastIndex)
         update(at, AppEventLog.history[at])
      else
         update(AppEventLog.history.lastIndex, AppEventLog.history.lastOrNull())
   }

   private fun updateIndexes() {
      historyAtText.value = when (uiErrorsOnly.value) {
         false -> (uiAt to AppEventLog.history.size)
         true -> (AppEventLog.history.asSequence().take(uiAt).count(::isError) to AppEventLog.history.count(::isError))
      }.net { (at, total) ->
         "${if (at<=0 && total==0) 0 else (at + 1)}/$total"
      }
   }

   private fun update(at: Int, event: Any?) {
      uiAt = at
      val text = event?.let {
         when (event) {
            is AppError -> event.textShort + "\n\n" + event.textFull
            is Throwable -> "Unspecified error: ${event.stacktraceAsString}"
            else -> event.toUi()
         }
      }
      uiText.text = text
      uiTextHeightUpdater(text ?: "")
      uiText.isWrapText = when (event) {
         is Throwable -> false
         is AppError -> false
         else -> true
      }
      uiText.textAlignment = when (event) {
         is Throwable -> TextAlignment.LEFT
         is AppError -> TextAlignment.LEFT
         else -> TextAlignment.CENTER
      }
      updateIndexes()
   }

   private fun isError(o: Any?) = o is Throwable || o is AppError
}