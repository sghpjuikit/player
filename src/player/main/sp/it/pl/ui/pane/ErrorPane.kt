package sp.it.pl.ui.pane

import javafx.beans.value.WritableValue
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.Pos.TOP_RIGHT
import javafx.geometry.VPos
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.Priority.NEVER
import javafx.scene.text.TextAlignment
import javafx.scene.text.TextBoundsType
import sp.it.pl.main.AppError
import sp.it.pl.main.AppEventLog
import sp.it.pl.main.IconFA
import sp.it.pl.main.Key
import sp.it.pl.main.emScaled
import sp.it.pl.main.toUi
import sp.it.pl.ui.objects.SpitText
import sp.it.pl.ui.objects.icon.CheckIcon
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.access.textAlign
import sp.it.util.access.toggleNext
import sp.it.util.access.v
import sp.it.util.dev.stacktraceAsString
import sp.it.util.functional.supplyIf
import sp.it.util.math.max
import sp.it.util.math.min
import sp.it.util.reactive.attach
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.zip
import sp.it.util.ui.Util.layScrollVTextCenter
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.minPrefMaxHeight
import sp.it.util.ui.setMinPrefMaxSize
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox

class ErrorPane: OverlayPane<Any>() {

   private val uiText: SpitText
   private var uiAt = -1
   private lateinit var historyAtText: WritableValue<String>
   private val uiErrorsOnly = v(true)
   private val uiTextAlignment = v(TextAlignment.CENTER)

   init {
      uiErrorsOnly attach { updateIndexes() }

      uiText = SpitText().apply {
         textOrigin = VPos.CENTER
         textAlignment = TextAlignment.CENTER
         boundsType = TextBoundsType.VISUAL
         setMinPrefMaxSize(-1.0)
      }

      content = stackPane {
         padding = Insets(50.emScaled, 300.emScaled, 50.emScaled, 300.emScaled)
         minPrefMaxHeight = 200.0.emScaled
         onEventDown(KEY_PRESSED, Key.LEFT) { visitLeft() }
         onEventDown(KEY_PRESSED, Key.RIGHT) { visitRight() }

         lay(CENTER) += vBox {
            isFillWidth = false
            alignment = CENTER
            lay += layScrollVTextCenter(uiText).apply {
               isFitToWidth = true
               minWidth = 400.emScaled
            }
         }
         lay(TOP_RIGHT) += vBox(0.0, CENTER_RIGHT) {
            isPickOnBounds = false
            isFillWidth = false

            lay(NEVER) += hBox(5.0, CENTER_RIGHT) {
               isPickOnBounds = false
               isFillHeight = false

               lay += label("Event Log")
               lay += CheckIcon(uiErrorsOnly)
               lay += Icon(IconFA.ANGLE_LEFT, -1.0, "Previous message").onClickDo { visitLeft() }
               lay += label("0/0") {
                  historyAtText = textProperty()
               }
               lay += Icon(IconFA.ANGLE_RIGHT, -1.0, "Next message").onClickDo { visitRight() }
               lay += Icon(null, -1.0, "Toggle text alignment").apply {
                  uiErrorsOnly zip uiTextAlignment sync { (isErrOnly, txtAlign) ->
                     uiText.textAlign.value = if (isErrOnly) TextAlignment.LEFT else txtAlign
                  }
                  uiText.textAlign sync {
                     val glyph = when (it!!) {
                        TextAlignment.CENTER -> IconFA.ALIGN_CENTER
                        TextAlignment.JUSTIFY -> IconFA.ALIGN_JUSTIFY
                        TextAlignment.RIGHT -> IconFA.ALIGN_RIGHT
                        TextAlignment.LEFT -> IconFA.ALIGN_LEFT
                     }
                     icon(glyph)
                  }
                  onClickDo { uiTextAlignment.toggleNext() }
               }
               lay += supplyIf(display.value!=Display.WINDOW) {
                  Icon(IconFA.SQUARE, -1.0, "Always on top\n\nForbid hiding this window behind other application windows").apply {
                     onClickDo {
                        stage?.let {
                           it.isAlwaysOnTop = !it.isAlwaysOnTop
                           icon(if (it.isAlwaysOnTop) IconFA.SQUARE else IconFA.SQUARE_ALT)
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
      if (!uiErrorsOnly.value) (uiAt max 1) - 1
      else AppEventLog.history.asSequence().withIndex().take(uiAt max 1).filter { (_, o) -> isError(o) }.lastOrNull()?.index
         ?: uiAt
   )

   private fun visitRight() = visit(
      if (!uiErrorsOnly.value) (uiAt min AppEventLog.history.size - 2) + 1
      else AppEventLog.history.asSequence().withIndex().drop(uiAt).filter { (_, o) -> isError(o) }.take(2).lastOrNull()?.index
         ?: uiAt
   )

   private fun visit(at: Int) = update(at, AppEventLog.history[at])

   private fun updateIndexes() {
      historyAtText.value =
         if (!uiErrorsOnly.value) "${uiAt + 1}/${AppEventLog.history.size}"
         else "${AppEventLog.history.asSequence().take(uiAt).count(::isError) + 1}/${AppEventLog.history.count(::isError)}"
   }

   private fun update(at: Int, error: Any) {
      uiAt = at
      uiText.text = when (error) {
         is AppError -> error.textShort + "\n\n" + error.textFull
         is Throwable -> "Unspecified error: ${error.stacktraceAsString}"
         else -> error.toUi()
      }
      updateIndexes()
   }

   private fun isError(o: Any?) = o is Throwable || o is AppError
}