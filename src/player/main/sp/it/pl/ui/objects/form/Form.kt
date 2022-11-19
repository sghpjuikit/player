package sp.it.pl.ui.objects.form

import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos.CENTER
import javafx.scene.control.ScrollPane
import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.layout.VBox
import sp.it.pl.main.IconFA
import sp.it.pl.main.emScaled
import sp.it.pl.main.okIcon
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.pane.ConfigPane
import sp.it.util.access.readOnly
import sp.it.util.access.transformValue
import sp.it.util.access.v
import sp.it.util.async.FX
import sp.it.util.async.future.Fut
import sp.it.util.conf.Configurable
import sp.it.util.dev.fail
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Error
import sp.it.util.functional.Try.Ok
import sp.it.util.functional.asIf
import sp.it.util.functional.getOr
import sp.it.util.functional.runTry
import sp.it.util.reactive.attach
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.scrollPane
import sp.it.util.ui.stackPane

/** Editor for [sp.it.util.conf.Configurable] - form. Has optional submit button. */
class Form(configurable: Configurable<*>, action: ((Configurable<*>) -> Any?)?): VBox(5.0) {

   private val okPane = hBox(0.0, CENTER)
   private val warnB = Icon(IconFA.WARNING)
   private val okB = okIcon { ok() }.apply { styleClass += "form-ok-button" }
   private var editorsPane = ConfigPane<Any?>()
   private val isExecutingCount = v(0)

   /** Configurable object. */
   val configurable = configurable
   /** Determines config editor order. Delegates to underlying [ConfigPane.editorOrder].*/
   var editorOrder by editorsPane::editorOrder
   /** Determines config editor ui. Delegates to underlying [ConfigPane.ui].*/
   val editorUi by editorsPane::ui
   /** Invoked when user submits the editing or programmatically by [ok]. Default does nothing.  See [ok].*/
   val onExecute = action ?: {}
   /** Invoked when execution finishes with or without exception. Default does nothing. See [ok].*/
   var onExecuteDone: (Try<*, Throwable>) -> Unit = {}
   /** Denotes whether config editors can be edited. Default true. */
   val isEditable = v(true)
   /** Denotes whether there is an action that user can execute. See [ok]. */
   val isExecutable = v(action!=null).readOnly()
   /** Denotes whether there is an action running. See [ok]. */
   val isExecuting = isExecutingCount.map { it>0 }.readOnly()
   /** Denotes whether multiple executions are allowed. If false and [[isExecuting]] is false, onOk button is disabled. */
   val isParallelExecutable = v(false)

   init {
      padding = Insets(5.0)
      lay += scrollPane {
         content = editorsPane
         isFitToWidth = true
         isFitToHeight = false
         vbarPolicy = AS_NEEDED
         hbarPolicy = NEVER
         consumeScrolling()
      }
      lay += stackPane(okPane) {
         prefHeight = 24.emScaled
      }

      updateOkButtonVisible()
      isParallelExecutable.attach { updateOkButtonVisible() }
      isExecuting.attach { updateOkButtonVisible() }

      editorsPane.editable syncFrom isEditable
      editorsPane.onChangeOrConstraint = { validate() }
      editorsPane.configure(this.configurable)

      validate()
   }

   /**
    * Return true iff calling [ok] is safe in respect to [isExecutable], [isParallelExecutable], [isExecuting].
    * Doesn't take [validate] into consideration.
    */
   fun okPermitted() = isExecutable.value && (isParallelExecutable.value || !isExecuting.value)

   /**
    * Invokes [okPermitted] - if it gives false, throws exception.
    * Invokes [validate] - if it gives [Error], does nothing.
    * Otherwise, invokes [onExecute] and subsequently [onExecuteDone]. In this time span [isExecuting] is true.
    */
   fun ok() {
      if (!okPermitted()) fail { "Not permitted, only executable if ${::okPermitted.name} gives true" }
      if (validate().isOk) {
         val resultTry = runTry {
            isExecutingCount.transformValue { it+1 }
            onExecute(configurable)
         }
         when (resultTry) {
            is Error<Throwable> -> {
               isExecutingCount.transformValue { it-1 }
               onExecuteDone(resultTry)
            }
            is Ok<*> -> {
               when (val result = resultTry.value) {
                  is Fut<*> -> {
                     result.onDone(FX) {
                        isExecutingCount.transformValue { it-1 }
                        onExecuteDone(it.toTry())
                     }
                  }
                  else -> {
                     isExecutingCount.transformValue { it-1 }
                     onExecuteDone(resultTry)
                  }
               }
            }
         }
      }
   }

   /**
    * Returns [Ok] if all editors for configs in [configurable] return [sp.it.pl.ui.itemnode.ConfigEditor.getValid] [Ok].
    * If [configurable] is [Validated], its [Validated.isValid] must also return [Ok].
    */
   fun validate(): Try<*, *> {
      val validation = run {
         editorsPane.getConfigEditors().asSequence()
            .map { e -> e.getValid().mapError { "${e.config.nameUi}: $it" } }
            .find { it.isError }
      } ?: run {
         if (configurable is Validated) configurable.isValid() else Try.ok()
      }

      updateOkButtonUsable(validation.isOk)
      showWarnButton(validation)
      return validation
   }

   fun focusFirstConfigEditor() = editorsPane.focusFirstConfigEditor()

   private fun showWarnButton(validation: Try<*, String>) {
      updateOkButtonUsable(validation.isOk)
      warnB.tooltip(validation.switch().map { "Validation\n\n$it" }.getOr(""))
      if (validation.isError) { if (warnB !in okPane.lay.children) okPane.lay += warnB }
      else okPane.lay -= warnB
   }

   private fun updateOkButtonUsable(isUsable: Boolean) {
      okB.isMouseTransparent = !isUsable
      okB.isFocusTraversable = isUsable
   }

   private fun updateOkButtonVisible() {
      if (okPermitted()) { if (okB !in okPane.children) okPane.lay += okB }
      else okPane.lay -= okB
   }

   override fun getContentBias() = Orientation.HORIZONTAL

   companion object {
      /**
       * @param configurable configurable object
       * @param onOk on submit action (taking the configurable as input parameter) or null if none. Submit button is
       * only visible if there is an action to execute.
       */
      fun <C: Configurable<*>> form(configurable: C, onOk: ((C) -> Any?)? = null) = Form(configurable, onOk.asIf())
   }
}

/** Marks [Configurable] that has additional validation to do before invoking [Form].[Form.onExecute]. */
interface Validated {
   fun isValid(): Try<*, String>
}