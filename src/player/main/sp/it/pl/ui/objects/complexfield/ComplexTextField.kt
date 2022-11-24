package sp.it.pl.ui.objects.complexfield

import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.control.TextInputControl
import javafx.scene.input.Clipboard
import javafx.scene.input.KeyCode.BACK_SPACE
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.FlowPane
import sp.it.pl.core.Parser
import sp.it.pl.core.Parser.ParsePartError
import sp.it.pl.core.Parser.ParsePartOk
import sp.it.pl.core.ParserArg
import sp.it.pl.core.UiStringHelper
import sp.it.pl.main.AppTexts.textNoVal
import sp.it.pl.main.Df
import sp.it.pl.main.Key
import sp.it.pl.main.getText
import sp.it.pl.main.hasText
import sp.it.pl.main.toS
import sp.it.pl.main.toUi
import sp.it.pl.ui.item_node.ConfigEditor
import sp.it.pl.ui.item_node.STYLECLASS_COMBOBOX_CONFIG_EDITOR
import sp.it.pl.ui.objects.SpitComboBox
import sp.it.util.access.readOnly
import sp.it.util.access.v
import sp.it.util.access.vn
import sp.it.util.collections.observableList
import sp.it.util.collections.setTo
import sp.it.util.conf.Config
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Ok
import sp.it.util.functional.asIf
import sp.it.util.functional.net
import sp.it.util.math.min
import sp.it.util.reactive.Handler1
import sp.it.util.reactive.attach
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.drag.set
import sp.it.util.ui.hasFocus
import sp.it.util.ui.lay
import sp.it.util.ui.styleclassToggle

/** [TextField], which can be filled with value with the help of [UiStringHelper]. */
open class ComplexTextField<T>(val parser: UiStringHelper<T>): FlowPane() {
   /** Behavior executing when value changes. */
   val onValueChange = Handler1<T?>()
   /** Whether user can (indirectly) edit [valueText]. */
   val isEditable = v(true)
   /** Text value representing the value. Read-write */
   private val valueTextRw = vn<String>(null)
   /** Text value representing the value. */
   val valueText = valueTextRw.readOnly()
   private val valuePos = v(-1)
   private val valueParts = observableList<String>()
   private val comboBox = SpitComboBox<ArgValue?>({ it?.valuePartUiName ?: textNoVal }, textNoVal)

   init {
      styleClass += STYLECLASS

      valueText attach { computeValue(it).ifOk(onValueChange) }
      valueParts.onChange { valueTextRw.value = valueParts.takeIf { it.isNotEmpty() }?.joinToString(" ") }

      comboBox.styleClass += STYLECLASS_COMBOBOX_CONFIG_EDITOR
      comboBox.valueProperty() attach { it?.buildUi() }
      valuePos attach { updateState() }
      valuePos.value = 0
      lay -= comboBox
      lay += comboBox

      // BACKSPACE removes last arg
      isFocusTraversable = true
      onEventDown(MOUSE_CLICKED, PRIMARY) { requestFocus() }
      onEventDown(KEY_PRESSED, BACK_SPACE) { clearLast() }
      comboBox.onEventDown(KEY_PRESSED, BACK_SPACE) { clearLast() }

      // copy/paste value
      onEventDown(KEY_PRESSED, Key.SHORTCUT, Key.V) { if (pasteValueAsTextPossible()) pasteValueAsText() }
      onEventDown(KEY_PRESSED, Key.SHORTCUT, Key.C) { copyValueAsText() }

      // Read-only mode disables
      isEditable sync { editable ->
         styleclassToggle("readonly", !editable)
         children.filterIsInstance<TagNode>().forEach { it.styleclassToggle("readonly", !editable) }
      }

   }

   override fun requestFocus() =
      children.asReversed().find { it.isFocusTraversable }?.requestFocus() ?: super.requestFocus()

   /** Invokes the action and retains focus that may have been lost */
   private fun retainingFocus(block: () -> Unit) {
      val f = hasFocus()
      block()
      if (f) requestFocus()
   }

   /** @return current value or error if value is not computable from current state */
   fun computeValue(text: String? = valueText.value): Try<T, String> =
      if (text==null) Try.ok(null as T) else parser.parse.parse(text) // TODO: make nullability safe

   /** Sets current state to reflect the specified value */
   fun updateValue(value: T) {
      val valueAsS = value?.toS()
      val (p, r) = when (valueAsS) {
         null -> null to null
         else -> parser.parse.parsers.firstNotNullOf { p -> p.parseWithErrorPositions(valueAsS).asIf<ParsePartOk<T>>()?.net { p to it } }
      }

      valueParts setTo (p?.args.orEmpty() zip r?.valueParts.orEmpty()).map { (arg, v) ->
         when (arg) {
            is ParserArg.Val -> arg.value
            is ParserArg.Arg<*> -> v.toS()
         }
      }
      valueTextRw.value = valueAsS
      valuePos.value = r?.at ?: 0
   }

   /** Copy current state to [Clipboard.getSystemClipboard] */
   fun copyValueAsText() {
      Clipboard.getSystemClipboard()[Df.PLAIN_TEXT] = valueTextRw.value ?: textNoVal
   }

   /** Set current state from [Clipboard.getSystemClipboard] */
   fun pasteValueAsTextPossible(): Boolean =
      Clipboard.getSystemClipboard().hasText()

   /** Set current state from [Clipboard.getSystemClipboard]. Must be checked first by [pasteValueAsTextPossible]. */
   fun pasteValueAsText(value: String = Clipboard.getSystemClipboard().getText()) {
      parser.parse.parse(value).ifOk(::updateValue)
   }

   fun clearLast() {
      if (hasFocus() && isEditable.value) {
         retainingFocus {
            if (valuePos.value == 0) clearValue()
            else valuePos.setValueOf { it - 1 }
         }
      }
   }

   fun clearValue() {
      if (isEditable.value) {
         retainingFocus {
            valueParts.clear()
            valueTextRw.value = null
            valuePos.value = 0
            updateState()
         }
      }
   }

   private fun updateState() {
      valueParts setTo valueParts.take(valuePos.value)
      children setTo children.take(valuePos.value)

      val at = valuePos.value
      val parseResultsRaw = parser.parse.parsers.map { it.parseWithErrorPositions(valueText.value ?: "") }
      val parseResults = (parser.parse.parsers zip parseResultsRaw).map { (p, parseResultRaw) ->
         when (parseResultRaw) {
            is ParsePartOk -> when (p.args.size) {
               valueParts.size -> parseResultRaw
               else -> ParsePartError(valuePos.value, "Missing argument") // it is possible to get ok result even if it not all args were specified by user yet
            }
            is ParsePartError -> parseResultRaw
         }
      }
      val parseResultValues = parseResults.mapNotNull { it.asIf<ParsePartOk<T>>() }.map { it.value }
      if (parseResultValues.isNotEmpty()) {
         lay -= comboBox

         val valueAsS = parseResultValues.first().toS()
         val (p, r) = parser.parse.parsers.firstNotNullOf { p -> p.parseWithErrorPositions(valueAsS).asIf<ParsePartOk<T>>()?.net { p to it } }
         for (i in children.size until r.at)
            argNode(p, p.args[i], i, true, r.valueParts[i]).buildUi()
      } else {
         val partialCandidates = parser.parse.parsers.mapIndexedNotNull { i, p ->
            when (val pr = parseResults[i]) {
               is ParsePartError ->
                  if (pr.at>=at && p.args.size>at) argNode(p, p.args[at], at)
                  else null
               is ParsePartOk -> null
            }
         }

         when (partialCandidates.size) {
            0 -> lay -= comboBox
            1 -> partialCandidates.first().buildUi()
            else -> {
               comboBox.items setTo (listOf(null) + partialCandidates)
               comboBox.value = comboBox.items.firstOrNull()
               lay += comboBox
            }
         }
      }
   }

   private fun argNode(p: Parser<*>, arg: ParserArg<*>, position: Int, avoidInit: Boolean = false, initialValue: Any? = null): ArgValue = when (arg) {
      is ParserArg.Val ->
         ArgValue(arg.value) { name ->
            retainingFocus {
               if (!avoidInit) valueParts setTo (valueParts.take(position) + name)
               lay -= comboBox
               lay += TagNode(arg.value)
               if (!avoidInit) valuePos.value = (position + 1) min p.args.lastIndex
            }
         }
      is ParserArg.Arg ->
         ArgValue("<${arg.type.toUi()}>") { name ->
            retainingFocus {
               lay -= comboBox
               lay += ConfigEditor.create(Config.forProperty(arg.type, name, vn(initialValue))).run {
                  isEditableAllowed syncFrom this@ComplexTextField.isEditable
                  onChangeOrConstraint = {
                     get().ifOk {
                        valueParts setTo (valueParts.take(position) + it.toS())
                        lay -= comboBox
                        valuePos.value = (position + 1) min p.args.lastIndex
                     }
                  }
                  // BACKSPACE removes last arg (editor may ignore BACKSPACE event so use it here)
                  editor.onEventUp(KEY_PRESSED, BACK_SPACE, false) {
                     val wasConsumedByEditor = it.isConsumed || editor is TextInputControl
                     val hasNoValue = get().net { it is Ok && it.value==null } || (editor.asIf<TextInputControl>()?.net { it.text.isNullOrEmpty() }==true && !isNullable)
                     if (hasNoValue || !wasConsumedByEditor) {
                        this@ComplexTextField.clearLast()
                        it.consume()
                     }
                  }
                  editor
               }
            }
         }
   }

   private data class ArgValue(val valuePartUiName: String, val buildUi: (String) -> Unit) {
      fun buildUi() = buildUi(valuePartUiName)
   }

   class TagNode(tagItem: String): Label(tagItem) {
      init { styleClass += STYLECLASS }

      companion object {
         const val STYLECLASS = "complex-text-field-tag"
      }
   }

   companion object {
      const val STYLECLASS = "complex-text-field"
   }

}