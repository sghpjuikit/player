package sp.it.pl.ui.itemnode

import java.util.function.BiPredicate
import java.util.function.Consumer
import java.util.stream.Stream
import javafx.event.Event
import javafx.geometry.Pos
import javafx.scene.control.ComboBox
import javafx.scene.control.TextArea
import javafx.scene.input.KeyCode.CONTROL
import javafx.scene.input.KeyCode.V
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Priority
import javafx.scene.layout.Priority.ALWAYS
import javafx.util.Callback
import sp.it.pl.main.appTooltip
import sp.it.pl.ui.itemnode.ListAreaNode.Transformation
import sp.it.pl.ui.itemnode.ListAreaNode.TransformationRaw
import sp.it.pl.ui.objects.SpitComboBox
import sp.it.util.Sort
import sp.it.util.Sort.ASCENDING
import sp.it.util.Sort.DESCENDING
import sp.it.util.Sort.NONE
import sp.it.util.access.not
import sp.it.util.access.v
import sp.it.util.access.vx
import sp.it.util.collections.collectionWrap
import sp.it.util.collections.getElementType
import sp.it.util.collections.list.PrefList
import sp.it.util.collections.materialize
import sp.it.util.collections.observableList
import sp.it.util.collections.readOnly
import sp.it.util.collections.setTo
import sp.it.util.collections.tabulate0
import sp.it.util.conf.AccessConfig
import sp.it.util.conf.Config
import sp.it.util.conf.Constraint
import sp.it.util.conf.Constraint.NumberMinMax
import sp.it.util.conf.EditMode
import sp.it.util.dev.fail
import sp.it.util.functional.FunctorPool
import sp.it.util.functional.Functors
import sp.it.util.functional.PF
import sp.it.util.functional.PF0
import sp.it.util.functional.PF1
import sp.it.util.functional.Parameter
import sp.it.util.functional.Parameterized
import sp.it.util.functional.TypeAwareF
import sp.it.util.functional.Util.IDENTITY
import sp.it.util.functional.Util.IS
import sp.it.util.functional.Util.IS0
import sp.it.util.functional.Util.ISNT
import sp.it.util.functional.Util.ISNT0
import sp.it.util.functional.asIs
import sp.it.util.functional.toUnit
import sp.it.util.math.max
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.attach
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sizes
import sp.it.util.reactive.suppressed
import sp.it.util.reactive.suppressing
import sp.it.util.reactive.suppressingAlways
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.zip
import sp.it.util.type.VType
import sp.it.util.type.estimateRuntimeType
import sp.it.util.type.isSubclassOf
import sp.it.util.type.isSubtypeOf
import sp.it.util.type.isSupertypeOf
import sp.it.util.type.type
import sp.it.util.type.typeNothingNonNull
import sp.it.util.ui.hBox
import sp.it.util.ui.install
import sp.it.util.ui.lay
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.vBox

/**
 * List editor with transformation ability. Editable area with function editor displaying the list contents.
 *
 * Allows:
 *  *  displaying the elements of the list in the text area
 *  *  manual editing of the text in the text area
 *  *  applying chain of function transformation on the list elements
 *
 * The input list is retained and all (if any) transformations are applied on it every time a change in the
 * transformation chain occurs. Manual tex edits are considered part of the transformation chain.
 *
 * The input can be set using [setInputToLinesOf] methods.
 *
 * The result can be accessed as:
 *  * concatenated text (which is equal to the visible text) [getValAsText]
 *  * list of strings [getVal]. Each string element represents a single line in the text area. [getVal]
 *  * list of objects [output]
 */
@Suppress("RemoveExplicitTypeArguments")
open class ListAreaNode: ValueNode<List<String>>(listOf()) {

   private val root = vBox()

   @JvmField protected val textArea = TextArea()

   /**
    * Value of this - a transformation input before transformation is applied.
    * See [setInput] and [setInputToLinesOf].
    *
    * Upon change:
    * - The [transforms] input element type is determined to the best of the ability.
    * - Transformation chain is cleared if the input element type has changed.
    * - Text of the text area is updated.
    */
   @JvmField val input = observableList<Any?>()

   /** The intermediate results of the [transforms], including [input] (first value) and [output] (last value). */
   private val transformsValues = mutableListOf<List<Any?>>()

   /** The transformation chain. */
   @JvmField val transforms = ListAreaNodeTransformations(transformsValues, Functors.pool)

   /** Writable [output]. */
   private val outputImpl = observableList<Any?>()

   /**
    * Value of this - a transformation output of the input list after transformation is applied to each
    * element. The text of this area shows string representation of this list.
    *
    * Note that the area is editable, but the changes will (and possibly could) only be reflected in this list only
    * if its type is [String], i.e., if the last transformation transforms into [String]. This is because this object
    * is a [String] transformer.
    *
    * When [outputText] is edited, then if:
    *  * output type is String: it is considered a transformation of that text, and it will be
    * reflected in this list, i.e., [getVal] and this will contain equal elements
    *  * output type is not String: it is considered arbitrary user change of the text representation
    * of transformation output (i.e., this list), but not the output itself.
    *
    * When further transforming the elements, the manual edit will always be ignored, i.e., only after-transformation
    * text edit will be considered.
    *
    * When observing this list, changes of the text area will only be reflected in it (and fire
    * list change events) when the output type is String. You may observe the text directly using
    * [outputText]
    */
   @JvmField val output = outputImpl.readOnly()

   /** Text of the text area and approximately concatenated [getVal]. Editable by user (ui) and programmatically. */
   @JvmField val outputText = textArea.textProperty()!!

   /** Prevents unwanted transformation updates when transformation chain changes */
   private val isTransformsChanging = Suppressor(false)

   init {
      input.onChange {
         transformsValues setTo listOf(input)
         transforms.typeIn = VType<Any?>(input.getElementType())
      }
      transforms.onItemChange = Consumer { transformations ->
         isTransformsChanging.suppressing {
            val i = transformations.mapNotNull { it as? Transformation.Manual }.lastOrNull()?.text?.lines() ?: input.materialize()
            val outputs = transformations.takeLastWhile { it !is Transformation.Manual }.runningFold(i) { it, transformation -> transformation(it) }
            transformsValues setTo (transformsValues.take(transformations.size + 1 - outputs.size) + outputs)
            val o = transformsValues.last()
            outputImpl setTo o
            val isManualEdit = transforms.chain.lastOrNull()?.chained?.getVal() is Transformation.Manual
            if (!isManualEdit) textArea.text = o.joinToString("\n")
            changeValue(textArea.text.lines())
         }
      }
      textArea.textProperty() attach { text ->
         isTransformsChanging.suppressed {
            val isManualEdit = transforms.chain.lastOrNull()?.chained?.getVal() is Transformation.Manual
            val isTextEdit = transforms.chain.isEmpty() || isManualEdit
            val transformation = TransformationRaw.Manual(text)
            val link = ListAreaNodeTransformationNode(PrefList<TransformationRaw>().apply { addPreferred(transformation) })

            if (isManualEdit) transforms.setChained(transforms.length() - 1, link)
            else transforms.addChained(link)

            if (isTextEdit) outputImpl setTo text.lines()
         }
      }
      textArea.onEventDown(KEY_PRESSED, CONTROL, V) { it.consume() } // avoids possible duplicate paste
      root.lay(ALWAYS) += textArea
      root.lay += transforms.getNode()
   }

   /** Sets the [input] list */
   open fun setInput(data: List<Any?>) {
      input setTo data
   }

   /** Sets the [input] list to lines of the specified text */
   fun setInputToLinesOf(text: String) = setInput(text.lines())

   override fun getNode() = root

   /** @return the value as text = [outputText] */
   fun getValAsText(): String = textArea.text

   sealed class TransformationRaw: Parameterized<Transformation, Any?> {
      abstract val name: String

      class Manual(val text: String): TransformationRaw() {
         override val name = "Manual edit"
         override val parameters = listOf<Parameter<Any?>>()
         override fun realize(args: List<*>) = Transformation.Manual(text)
      }

      class ByString(name: String, val f: PF<String, Any?>): TransformationRaw() {
         override val name = "text: $name"
         override val parameters = f.parameters
         override fun realize(args: List<*>) = Transformation.ByString(name, f.realize(args))
      }

      // outputType must match type of list of output of f
      // outputType is necessary because output list element type is erased
      class By1(val inputType: VType<*>, val outputType: VType<*>, val f: PF<List<*>, List<*>>): TransformationRaw() {
         override val name = "list: ${f.name}"
         override val parameters = f.parameters
         override fun realize(args: List<*>) = Transformation.By1(f.name, inputType, outputType, f.realize(args))
      }

      class ByN(val f: PF<Any?, Any?>): TransformationRaw() {
         override val name = "each: ${f.name}"
         override val parameters = f.parameters
         override fun realize(args: List<*>) = Transformation.ByN(f.name, f.realize(args))
      }

      class ByValue(val inputType: VType<*>, val outputType: VType<*>, val f: PF<List<*>, List<*>>): TransformationRaw() {
         override val name = "value: ${f.name}"
         override val parameters = f.parameters
         override fun realize(args: List<*>) = Transformation.By1(f.name, inputType, outputType, f.realize(args))
      }

   }

   sealed class Transformation {
      abstract val name: String
      abstract val linkTypeIn: VType<*>?
      abstract val linkTypeOut: VType<*>?
      abstract operator fun invoke(data: List<Any?>): List<Any?>

      object AsIs: Transformation() {
         override val name = "As is"
         override val linkTypeIn = null
         override val linkTypeOut = null
         override fun invoke(data: List<Any?>) = data
      }

      class Manual(val text: String): Transformation() {
         override val name = "Manual edit"
         override val linkTypeIn = type<Any?>()
         override val linkTypeOut = type<String>()
         override fun invoke(data: List<Any?>) = text.lines()
      }

      class ByString(override val name: String, val f: (String) -> Any?): Transformation() {
         override val linkTypeIn = type<String>()
         override val linkTypeOut = type<String>()
         override fun invoke(data: List<Any?>) = data.joinToString("\n").let(f).let(::collectionWrap).toList()
      }

      class By1(override val name: String, override val linkTypeIn: VType<*>, override val linkTypeOut: VType<*>, val f: (List<*>) -> List<*>): Transformation() {
         override fun invoke(data: List<Any?>) = data.let(f)
      }

      class ByN(override val name: String, val f: (Any?) -> Any?): Transformation() {
         override val linkTypeIn: VType<*>?
            get() = when (f) {
               IDENTITY -> null
               IS, ISNT, IS0, ISNT0 -> type<Any?>()
               is TypeAwareF<*, *> -> when (f.f) {
                  IDENTITY -> null
                  IS, ISNT, IS0, ISNT0 -> type<Any?>()
                  else -> f.typeIn
               }
               else -> fail { "Unrecognized function $f" }
            }
         override val linkTypeOut: VType<*>?
            get() = when (f) {
               IDENTITY -> null
               IS, ISNT, IS0, ISNT0 -> type<Boolean>()
               is TypeAwareF<*, *> -> when (f.f) {
                  IDENTITY -> null
                  IS, ISNT, IS0, ISNT0 -> type<Boolean>()
                  else -> f.typeOut
               }
               else -> fail { "Unrecognized function $f" }
            }

         override fun invoke(data: List<Any?>) = data.map(f)
      }

   }
}

/** [ FChainItemNode] adjusted for [ListAreaNode] */
class ListAreaNodeTransformations: ChainValueNode<Transformation, ListAreaNodeTransformationNode, List<Transformation>> {

   @Suppress("RemoveExplicitTypeArguments")
   constructor(transformsValues: MutableList<List<Any?>>, functions: FunctorPool): super(listOf()) {
      chainedFactory = Callback { i ->
         val typeFunctionOut = typeOut
         val typeRuntimeOut = transformsValues[i].estimateRuntimeType()
         val type = if (typeRuntimeOut isSubtypeOf typeFunctionOut) typeRuntimeOut else typeFunctionOut

         val byNs = functions.getI(type).asIs<PrefList<PF<Any?, Any?>>>()
            .apply { removeIf { it.name=="#" } }
            .map<TransformationRaw> { TransformationRaw.ByN(it) }
         val all = byNs.apply {
            // By1
            this += TransformationRaw.By1(type<Any?>(), type<Int>(), PF0("# (0)", type<List<Any?>>(), type<List<Int>>()) { it.indices.toList() })
            this += TransformationRaw.By1(type<Any?>(), type<Int>(), PF0("# (1)", type<List<Any?>>(), type<List<Int>>()) { it.indices.map { it + 1 } })
            this += TransformationRaw.By1(type<Any?>(), type<Any?>(), PF0("Filter non null", type<List<Any?>>(), type<List<Any>>()) { it.filterNotNull() })
            this += TransformationRaw.By1(type<Any?>(), type<Any?>(), PF0("Type (nearest supertype)", type<List<Any?>>(), type<List<VType<Any>>>()) { listOf(typeRuntimeOut) })
            this += TransformationRaw.By1(type<Any?>(), type<Any?>(), PF0("Type (of last function)", type<List<Any?>>(), type<List<VType<Any>>>()) { listOf(typeFunctionOut) })
            this += TransformationRaw.By1(
               type, type,
               PF1("Repeat elements", type<List<Any?>>(), type<List<Any?>>(), Parameter("Times", null, type<Int>(), 1, setOf(NumberMinMax(0.0, null)))) { it, times ->
                  it.flatMap { tabulate0(times) { _ -> it } }
               }
            )

            // ByValue
            val separator = Parameter("Separator", "Separator", type<String>(), "", setOf(Constraint.Multiline))
            this += TransformationRaw.ByValue(type<Any?>(), type<Any?>(), PF0("join", type<List<Any?>>(), type<List<Int>>()) { listOf(it) })
            this += TransformationRaw.ByValue(type<Any?>(), type<Any?>(), PF1("join to text", type<List<Any?>>(), type<List<Any?>>(), separator) { it, s -> listOf(it.joinToString(s)) })
            this += TransformationRaw.ByValue(type<Any?>(), type<Any?>(), PF0("flatten", type<List<Any?>>(), type<List<Any?>>()) { it.flatMap { if (it is Collection<*>) it else if (it is Map<*,*>) it.entries else listOf(it) } })

            // ByString
            this += functions.getIO(type<String>(), type<Any?>()).map {
               TransformationRaw.ByString(it.name, it)
            }

            // By1 Comparable
            if (type.isSubclassOf<Comparable<*>>()) {
               val pSort = Parameter(type<Sort>(), ASCENDING)
               this += TransformationRaw.By1(type, type, PF1("Sort (naturally)", type<List<*>>(), type<List<*>>(), pSort) { it, sort ->
                  when (sort) {
                     NONE -> it.asIs()
                     ASCENDING -> it.asIs<List<Comparable<Any?>>>().asIterable().sorted().asIs()
                     DESCENDING -> it.asIs<List<Comparable<Any?>>>().asIterable().sorted().reversed().asIs()
                  }
               })
            }


            // requires some work
            // val sorters = APP.classFields[typeOut].filter { it.type.isSuperclassOf<Comparable<*>>() }
            // if (sorters.isNotEmpty()) {
            //    val pBy = Parameter(type<Sort>(), ObjectField)
            //    val pSort = Parameter(type<Sort>(), ASCENDING)
            //    this += TransformationRaw.By1(type, PF2("Sort by", jClass<List<*>>(), jClass<List<*>>(), pBy, pSort) { it, by, sort -> ??? })
            // }
         }

         ListAreaNodeTransformationNode(all).apply {
            onRawFunctionChange = {
               chain.forEach { it.updateIcons() }
               chain.forEachIndexed { i, c -> c.chained.isEditableRawFunction.value = chain.lastIndex==i }
            }
         }
      }
      isHomogeneousRem = BiPredicate { i, _ -> linkTypeInAt(i + 1) isSupertypeOf linkTypeOutAt(i - 1) }
      isHomogeneousAdd = BiPredicate { i, _ -> i==chain.size - 1 }
      isHomogeneousOn = BiPredicate { _, transformation -> transformation !is Transformation.Manual }
      isHomogeneousEdit = BiPredicate { _, _ -> true }
      maxChainLength attach {
         val m: Int = it.toInt()
         if (m<chain.size) {
            chain setTo chain.subList(0, m)
            generateValue()
         }
      }
      maxChainLength.value = Integer.MAX_VALUE
      chain.onChange { chain.forEach { it.updateIcons() } }
      chain.onChange { chain.forEachIndexed { i, it -> it.chained.isEditableRawFunction.value = chain.lastIndex==i } }
      inconsistentState = false
      generateValue()
   }

   /**
    * Sets input type.
    * Input type determines:
    *  * the input type of this chain's reduction function
    *  * input type of the first transformation function in the chain
    *
    * Default non nullable [Nothing]
    */
   var typeIn: VType<*> = typeNothingNonNull()
      set(value) {
         if (value==field) {
            generateValue()
         } else {
            field = value
            inconsistentState = true
            chain.clear()
            inconsistentState = false
            changeValueAlways(listOf())
         }
      }

   val typeOut: VType<*>
      get() = chain.map { it.chained.getVal().linkTypeOut }.fold(typeIn) { i, o -> o ?: i }

   private fun linkTypeInAt(at: Int) = chain.asSequence().drop(at).mapNotNull { it.chained.getVal().linkTypeIn }.firstOrNull()
      ?: type<Any?>()

   private fun linkTypeOutAt(at: Int) = chain.asSequence().take((at + 1) max 0).mapNotNull { it.chained.getVal().linkTypeOut }.lastOrNull()
      ?: typeIn

   override fun reduce(values: Stream<Transformation>) = values.toList()

}

/** [FItemNode] for [ListAreaNode]. */
class ListAreaNodeTransformationNode(transformations: PrefList<TransformationRaw>): ValueNode<Transformation>(Transformation.AsIs) {
   private val root = hBox(5, Pos.CENTER_LEFT).apply { id = "fItemNodeRoot" }
   private val paramB = hBox(5, Pos.CENTER_LEFT).apply { id = "fItemNodeParamsRoot" }
   private val editors = ArrayList<ConfigEditor<*>>()
   private val fCB: ComboBox<TransformationRaw>
   private var avoidGenerateValue = Suppressor(false)
   var isEditableRawFunction = v(true)
   var onRawFunctionChange = { _: TransformationRaw? -> }

   init {
      avoidGenerateValue.suppressingAlways {
         fCB = SpitComboBox({ it.name })
         fCB.items setTo transformations.sortedBy { it.name }
         fCB.value = transformations.preferredOrFirst
         fCB.disableProperty() syncFrom isEditableRawFunction.not()
         // display non-editable as label
         isEditableRawFunction zip fCB.items.sizes() sync { (editable, itemCount) ->
            fCB.pseudoClassChanged("editable", editable && itemCount.toInt()>1)
         }
         // display TransformationRaw.Manual as label
         fCB.onEventUp(Event.ANY) {
            if (transformations.size==1 && transformations.firstOrNull() is TransformationRaw.Manual && (it is KeyEvent || it is MouseEvent))
               it.consume()
         }
         fCB.valueProperty() sync { transformationRaw ->
            editors.clear()
            paramB.children.clear()
            transformationRaw?.parameters.orEmpty().forEachIndexed { i, p ->
               val editor = p.toConfig { generateValue() }.createEditor().apply {
                  if (p.description.isNotBlank())
                     editor install appTooltip(p.description)
               }
               editors += editor
               paramB.lay(if (i==0) ALWAYS else Priority.SOMETIMES) += editor.buildNode(false)
            }
            onRawFunctionChange(transformationRaw)
            generateValue()
         }
      }
      generateValue()

      root.lay += fCB
      root.lay(ALWAYS) += paramB
   }

   override fun getNode() = root

   override fun focus() = editors.firstOrNull()?.focusEditor().toUnit()

   private fun generateValue() {
      avoidGenerateValue.suppressed {
         val transformationRaw = fCB.value ?: null
         val parameters = editors.map { it.config.value }
         val transformation = transformationRaw?.realize(parameters)
         if (transformation!=null) changeValue(transformation)
      }
   }

   fun clear() {
      avoidGenerateValue.suppressing {
         editors.forEach { it.refreshDefaultValue() }
      }
      generateValue()
   }

   companion object {

      private fun <T> Config<T>.createEditor() = ConfigEditor.create(this)

      private fun <T> Parameter<T>.toConfig(onChange: (T?) -> Unit): Config<T> {
         val a = vx(defaultValue).apply { attach { onChange(it) } }
         return AccessConfig(type, name, name, { a.value = it }, { a.value }, "", description, EditMode.USER).addConstraints(constraints)
      }

   }
}