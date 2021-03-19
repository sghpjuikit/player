package sp.it.pl.ui.objects.form

import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos.CENTER
import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.text.TextAlignment
import sp.it.pl.main.okIcon
import sp.it.pl.ui.objects.TextFlowWithNoWidth
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
import sp.it.util.reactive.map
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.scrollPane
import sp.it.util.ui.text
import sp.it.util.ui.x

/** Editor for [sp.it.util.conf.Configurable] - form. Has optional submit button. */
class Form(configurable: Configurable<*>, action: ((Configurable<*>) -> Any?)?): VBox(5.0) {

   private val okPane = StackPane()
   private val warnText = text()
   private val warnFlow = TextFlowWithNoWidth()
   private val okB = okIcon { ok() }
   private var editorsPane = ConfigPane<Any?>()
   private val anchorOk = 90.0
   private val anchorWarn = 20.0
   private val isExecutingCount = v(0)

   /** Configurable object. */
   val configurable = configurable
   /** Determines config editor order. Delegates to underlying [ConfigPane.editorOrder].*/
   var editorOrder by editorsPane::editorOrder
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
         vbarPolicy = AS_NEEDED
         hbarPolicy = AS_NEEDED
         consumeScrolling()
      }
      lay += okPane.apply {
         prefSize = 30 x 30
      }

      warnFlow.apply {
         lay += warnText.apply {
            textAlignment = TextAlignment.CENTER
            BorderPane.setAlignment(this, CENTER)
         }
      }

      updateOkButtonVisible()
      isParallelExecutable.attach { updateOkButtonVisible() }
      isExecuting.attach { updateOkButtonVisible() }

      editorsPane.editable syncFrom isEditable
      editorsPane.onChangeOrConstraint = Runnable { validate() }
      editorsPane.configure(this.configurable)

      validate()
   }

   /**
    * Return true iff calling [ok] is safe in respect to [isExecutable], [isParallelExecutable], [isExecuting].
    * Doesn't take [validate] into consideration.
    */
   fun okPermitted() = isExecutable.value && (isParallelExecutable.value || !isExecuting.value)

   /**
    * Invokes [okPermitted] - if gives false, throws exception.
    * Invokes [validate] - if gives [Error], does nothing.
    * Otherwise invokes [onExecute] and subsequently [onExecuteDone]. In this time span [isExecuting] is true.
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
      warnText.text = validation.switch().map { "Form contains wrong data: $it" }.getOr("")
      if (validation.isError) { if (warnFlow !in children) children += warnFlow }
      else children -= warnFlow
   }

   private fun updateOkButtonUsable(isUsable: Boolean) {
      okB.isMouseTransparent = !isUsable
      okB.isFocusTraversable = isUsable
   }

   private fun updateOkButtonVisible() {
      if (okPermitted()) { if (okB !in okPane.children) okPane.lay(CENTER) += okB }
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