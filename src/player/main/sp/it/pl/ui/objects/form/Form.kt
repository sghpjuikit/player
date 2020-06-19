package sp.it.pl.ui.objects.form

import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import javafx.scene.text.TextAlignment
import sp.it.pl.ui.pane.ConfigPane
import sp.it.pl.main.okIcon
import sp.it.util.access.v
import sp.it.util.conf.Configurable
import sp.it.util.functional.Try
import sp.it.util.functional.asIf
import sp.it.util.reactive.consumeScrolling
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.scrollPane
import sp.it.util.ui.x

/**
 * [sp.it.util.conf.Configurable] editor. Has optional submit button.
 *
 * @param <T> type of the [sp.it.util.conf.Configurable] for this component.
 */
class Form<T>: AnchorPane {

   private val buttonPane = BorderPane()
   private val okPane = StackPane()
   private val fieldsPane = StackPane()
   private val warnLabel = Label()
   private val okB = okIcon { ok() }
   private var editorsPane = ConfigPane<T>()
   private val anchorOk = 90.0
   private val anchorWarn = 20.0
   /** Configurable object. */
   val configurable: Configurable<T>
   /** Invoked when user submits the editing. Default does nothing. */
   val onOK: (Configurable<T>) -> Unit
   /** Denotes whether there is an action that user can execute. */
   val hasAction = v(false)

   private constructor(c: Configurable<T>, onOk: ((Configurable<T>) -> Unit)?): super() {
      configurable = c
      onOK = onOk ?: {}
      hasAction.value = onOk!=null

      padding = Insets(5.0)
      lay(0, 0, anchorOk, 0) += fieldsPane
      lay(null, 0, 0, 0) += buttonPane.apply {
         prefHeight = 30.0
         center = okPane.apply {
            prefSize = 30 x 30
            BorderPane.setAlignment(this, CENTER)
         }
         bottom = warnLabel.apply {
            textAlignment = TextAlignment.CENTER
            BorderPane.setAlignment(this, CENTER)
         }
      }

      fieldsPane.lay += scrollPane {
         content = editorsPane
         isFitToWidth = true
         vbarPolicy = AS_NEEDED
         hbarPolicy = AS_NEEDED
      }
      okPane.lay += okB

      showOkButton(hasAction.value)

      editorsPane.onChange = Runnable { validate() }
      editorsPane.configure(configurable)

      fieldsPane.consumeScrolling()

      validate()
   }

   fun ok() {
      validate().ifOk {
         if (hasAction.value) onOK(configurable)
      }
   }

   fun focusFirstConfigEditor() = editorsPane.focusFirstConfigEditor()

   private fun validate(): Try<*, *> {
      val validation = run {
         editorsPane.getConfigEditors().asSequence()
            .map { e -> e.getValid().mapError { "${e.config.nameUi}: $it" } }
            .find { it.isError }
      } ?: run {
         if (configurable is Validated) configurable.isValid()
         else Try.ok()
      }
      showWarnButton(validation)
      return validation
   }

   private fun showWarnButton(validation: Try<*, String>) {
      okB.isMouseTransparent = validation.isError
      buttonPane.bottom = if (validation.isOk) null else warnLabel
      updateAnchor()
      warnLabel.text = if (validation.isError) "Form contains wrong data: ${validation.errorOrThrow}" else ""
   }

   private fun showOkButton(visible: Boolean) {
      buttonPane.isVisible = visible
      updateAnchor()
   }

   private fun updateAnchor() {
      val isOkVisible = buttonPane.isVisible
      val isWarnVisible = buttonPane.bottom!=null
      val a = (if (isOkVisible) anchorOk else 0.0) + if (isWarnVisible) 20.0 else 0.0
      setBottomAnchor(fieldsPane, a)
   }

   companion object {
      /**
       * @param configurable configurable object
       * @param onOk on submit action (taking the configurable as input parameter) or null if none. Submit button is
       * only visible if there is an action to execute.
       */
      @JvmOverloads
      fun <C: Configurable<*>> form(configurable: C, onOk: ((C) -> Unit)? = null) = Form(configurable, onOk.asIf())
   }
}

/** Marks [Configurable] that has additional validation to do before invoking [Form].[Form.onOK]. */
interface Validated {
   fun isValid(): Try<*, String>
}