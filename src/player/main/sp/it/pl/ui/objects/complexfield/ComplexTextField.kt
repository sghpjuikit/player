package sp.it.pl.ui.objects.complexfield

import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode.BACK_SPACE
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.FlowPane
import sp.it.pl.core.Parser
import sp.it.pl.core.ParserArg
import sp.it.pl.core.UiStringHelper
import sp.it.pl.main.AppTexts.textNoVal
import sp.it.pl.main.toS
import sp.it.pl.main.toUi
import sp.it.pl.ui.item_node.ConfigEditor
import sp.it.pl.ui.item_node.STYLECLASS_COMBOBOX_CONFIG_EDITOR
import sp.it.pl.ui.objects.SpitComboBox
import sp.it.util.access.readOnly
import sp.it.util.access.v
import sp.it.util.access.vn
import sp.it.util.async.runLater
import sp.it.util.collections.observableList
import sp.it.util.collections.setTo
import sp.it.util.conf.Config
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Ok
import sp.it.util.functional.and
import sp.it.util.math.max
import sp.it.util.reactive.Handler1
import sp.it.util.reactive.attach
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.hasFocus
import sp.it.util.ui.lay
import sp.it.util.ui.styleclassToggle

private typealias Error<T> = Try.Error<Pair<T, String>>
private typealias ArgValue = Pair<String, () -> Unit>

/** [TextField], which can be filled with value with the help of [UiStringHelper]. */
open class ComplexTextField<T>(val parser: UiStringHelper<T>): FlowPane() {
   /** Behavior executing when value changes. */
   val onValueChange = Handler1<T>()
   /** Whether user can (indirectly) edit [valueText]. */
   val isEditable = v(true)
   /** Text value representing the value. Read-write */
   private val valueTextRw = v("")
   /** Text value representing the value. */
   val valueText = valueTextRw.readOnly()
   private val valuePosition = v(-1)
   private val valuePartials = observableList<String>()
   private val comboBox = SpitComboBox<Pair<String, () -> Unit>?>({ it?.first  ?: textNoVal }, textNoVal)

   init {
      styleClass += STYLECLASS

      valueText attach { parser.parse.parse(it).ifOk(onValueChange) }

      valuePartials.onChange {
         valueTextRw.value = valuePartials.joinToString(" ")
      }

      comboBox.styleClass += STYLECLASS_COMBOBOX_CONFIG_EDITOR
      comboBox.valueProperty() attach {
         it?.second?.invoke()
      }
      valuePosition attach { position ->
         valuePartials setTo valuePartials.take(position)
         children setTo children.take(position)

         val parseResultsRaw = parser.parse.parsers.map { it.parseWithErrorPositions(valueText.value) }
         val parseResults = (parser.parse.parsers zip parseResultsRaw).map { (p, parseResultRaw) ->
            parseResultRaw.and {
               if (p.args.size==valuePartials.size) Try.ok()
               else Try.error(valuePosition.value to "Missing argument") // it is possible to get ok result even if it not all args were specified by user yet
            }
         }
         val parseResultOks = parseResults.filterIsInstance<Ok<T>>().map { it.value }

         if (parseResultOks.isNotEmpty()) {
            lay -= comboBox
         } else {
            val partialCandidates = parser.parse.parsers.mapIndexedNotNull { i, p ->
               when (val pr = parseResults[i]) {
                  is Ok<T> -> null
                  is Error<Int> -> {
                     if (pr.value.first>=position && p.args.size>position) argNode(p, p.args[position], position)
                     else null
                  }
               }
            }

            when (partialCandidates.size) {
               0 -> lay -= comboBox
               1 -> partialCandidates.first().second()
               else -> {
                  comboBox.items setTo (listOf(null) + partialCandidates)
                  lay += comboBox
               }
            }
         }
      }
      valuePosition.value = 0
      runLater {
         lay -= comboBox
         lay += comboBox
      }

      // BACKSPACE removes last arg
      isFocusTraversable = true
      val clearLast = {
         if (hasFocus() && isEditable.value) {
            valuePosition.setValueOf { (it - 1) max 0 }
            requestFocus()
         }
      }
      onEventDown(MOUSE_CLICKED, PRIMARY) { requestFocus() }
      onEventDown(KEY_PRESSED, BACK_SPACE) { clearLast() }
      comboBox.onEventDown(KEY_PRESSED, BACK_SPACE) { clearLast() }

      // Read-only mode disables
      isEditable sync { editable ->
         styleclassToggle("readonly", !editable)
         children.filterIsInstance<TagNode>().forEach { it.styleclassToggle("readonly", !editable) }
      }

   }

   fun computeValue(text: String = valueText.value): Try<T, String> = parser.parse.parse(text)

   fun updateValue(value: T) {
      valueTextRw.value = value.toS()
   }

   fun clearValue() {
      valuePartials.clear()
      valueTextRw.value = ""
      valuePosition.value = 0
   }

   @Suppress("UNUSED_PARAMETER")
   private fun <A> argNode(p: Parser<T>, arg: ParserArg<A>, position: Int, initialValue: A? = null): ArgValue =
      when (arg) {
         is ParserArg.Val -> arg.value to {
            valuePartials setTo (valuePartials.take(position) + arg.value)
            lay -= comboBox
            lay += TagNode(arg.value)
            valuePosition.value = position + 1
         }
         is ParserArg.Arg<A> -> {
            val name = "<${arg.toUi()}>"
            name to {
               lay -= comboBox
               lay += ConfigEditor.create(Config.forProperty(arg.type, name, vn(initialValue))).run {
                  isEditableAllowed syncFrom this@ComplexTextField.isEditable
                  onChangeOrConstraint = {
                     get().ifOk {
                        valuePartials setTo (valuePartials.take(position) + it.toS())
                        lay -= comboBox
                        valuePosition.value = position + 1
                     }
                  }
                  editor
               }
            }
         }
      }

   class TagNode(tagItem: String): Label(tagItem) {
      init {
         styleClass += STYLECLASS
      }

      companion object {
         const val STYLECLASS = "complex-text-field-tag"
      }
   }

   companion object {
      const val STYLECLASS = "complex-text-field"
   }

}