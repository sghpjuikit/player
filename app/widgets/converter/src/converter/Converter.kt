package converter

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.nio.file.Files
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.Side
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.IndexRange
import javafx.scene.control.Label
import javafx.scene.input.Clipboard
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.VBox
import kotlin.reflect.KClass
import kotlin.streams.asSequence
import kotlinx.coroutines.invoke
import sp.it.pl.audio.Song
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.MetadataWriter
import sp.it.pl.audio.tagging.read
import sp.it.pl.audio.tagging.writeNoRefresh
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.layout.feature.Opener
import sp.it.pl.layout.feature.SongWriter
import sp.it.pl.main.APP
import sp.it.pl.main.AppProgress
import sp.it.pl.main.AppTexts.textNoVal
import sp.it.pl.main.Css
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.IconOC
import sp.it.pl.main.WidgetTags.AUDIO
import sp.it.pl.main.WidgetTags.UTILITY
import sp.it.pl.main.Widgets.CONVERTER_NAME
import sp.it.pl.main.emScaled
import sp.it.pl.main.getAny
import sp.it.pl.main.installDrag
import sp.it.pl.main.showFloating
import sp.it.pl.main.toUi
import sp.it.pl.ui.item_node.ChainValueNode.ListChainValueNode
import sp.it.pl.ui.item_node.ConfigEditor.Companion.create
import sp.it.pl.ui.item_node.ListAreaNode
import sp.it.pl.ui.item_node.ValueNode
import sp.it.pl.ui.objects.SpitComboBox
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.window.ShowArea.WINDOW_ACTIVE
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.pl.ui.pane.ConfigPane
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.Util.filenamizeString
import sp.it.util.access.focused
import sp.it.util.access.v
import sp.it.util.access.vn
import sp.it.util.async.coroutine.FX
import sp.it.util.async.coroutine.IO
import sp.it.util.async.coroutine.launch
import sp.it.util.async.future.Fut.Result.ResultFail
import sp.it.util.async.future.Fut.Result.ResultOk
import sp.it.util.collections.map.KClassListMap
import sp.it.util.collections.materialize
import sp.it.util.collections.observableList
import sp.it.util.collections.setTo
import sp.it.util.conf.Config.Companion.forProperty
import sp.it.util.conf.Constraint.PreserveOrder
import sp.it.util.conf.Constraint.ValueSealedSet
import sp.it.util.dev.failIf
import sp.it.util.file.renameFile
import sp.it.util.file.renameFileNoSuffix
import sp.it.util.functional.Util.findFirstInt
import sp.it.util.functional.Util.split
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.runTry
import sp.it.util.functional.toUnit
import sp.it.util.reactive.attach
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onChangeAndNow
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync
import sp.it.util.text.capitalLower
import sp.it.util.text.chars32
import sp.it.util.type.VType
import sp.it.util.type.estimateRuntimeType
import sp.it.util.type.raw
import sp.it.util.type.type
import sp.it.util.ui.borderPane
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.menuItem
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.text
import sp.it.util.ui.textFlow
import sp.it.util.ui.vBox
import sp.it.util.units.version
import sp.it.util.units.year

class Converter(widget: Widget): SimpleController(widget), Opener, SongWriter {
   private val source = observableList<Any?>()
   private val taIn = EditAreaMain()
   private val tas = observableList<EditArea>(taIn)
   private val acts = KClassListMap<Act<*>>({ it.type })
   private val outTFBox = HBox(10.emScaled)
   private var applier: Applier? = null
   private var applierPopup: PopWindow? = null
   private val inputValue = io.i.create("Value", type<Any?>(), null) { source setTo unpackData(it) }

   init {
      root.setPrefSize(800.emScaled, 500.emScaled)

      // layout
      root.lay += hBox(10.emScaled) {
         lay(ALWAYS) += taIn.getNode()
         lay += hBox() {
            lay += outTFBox
            lay += vBox(0.0, Pos.TOP_LEFT) {
               lay(ALWAYS) += Icon().blank()
               lay += Icon(IconFA.PLAY_CIRCLE)
                  .tooltip("Use data\n\nOpen dialog for using the current data")
                  .onClickDo { applierShowPopup() }
            }
         }
      }
      tas.onChange { outTFBox.children setTo tas.map { it.getNode() } }

      // drag&drop
      root.installDrag(IconFA.LIST_ALT, { "Set data as input" },
         { true },
         { false },
         { inputValue.value = it.dragboard.getAny() },
         { taIn.getNode().layoutBounds }
      )

      // on source change run transformation
      source.onChange { taIn.setInput(source) }

      // add actions
      // Note that the actions are looked up (per class) in the order they are inserted. Per each
      // class, the first added action will become 'default' and show up as selected when data is
      // set.
      // Therefore, the order below matters.
      acts.accumulate(
         Act<File>(
            "Rename files",
            File::class,
            1,
            listOf("Filename"),
            { file, data ->
               IO {
                  file.renameFileNoSuffix(data.get("Filename")!!)
               }
            }
         )
      )
      acts.accumulate(
         Act<File>(
            "Rename files (and extension)",
            File::class,
            1,
            listOf("Filename"),
            { file, data ->
               IO {
                  file.renameFile(data.get("Filename")!!)
               }
            }
         )
      )
      acts.accumulate(
         object: Act<Song>(
            "Edit song tags",
            Song::class,
            100,
            Metadata.Field.all.map { it.name() },
            null
         ) {
            init {
               isInputsFixedLength = false
               actionImpartial = { data ->
                  var songs = source.filterIsInstance<Song>()
                  if (songs.isNotEmpty()) {
                     failIf(data.values.any { it.size!=songs.size }) { "Data size mismatch" }
                     IO {
                        for (i in songs.indices) {
                           var j = i
                           songs.get(i).writeNoRefresh { w: MetadataWriter ->
                              data.forEach { field: String?, values: List<String?> ->
                                 w.setFieldS(
                                    Metadata.Field.valueOf(field!!), values.get(j)
                                 )
                              }
                           }
                        }
                        APP.audio.refreshSongsWith(songs.map { it.read() }.filter { !it.isEmpty() }.toList())
                     }
                  } else
                     Unit
               }
            }
         }
      )
      acts.accumulate(WriteFileAct())
      acts.accumulate(ActCreateDirs())

      var outputAsText = io.o.create("Output (as text)", type<String>(), "")
      var output = io.o.create("Output", type<List<Any?>>(), listOf())
      taIn.outputText attach { outputAsText.value = it.orEmpty() }
      taIn.output.onChange { output.value = taIn.output.materialize() }
   }

   override fun read(song: Song?) {
      super<SongWriter>.read(song)
   }

   override fun read(songs: List<Song>) {
      inputValue.value = songs.map { it.toMeta() }
   }

   override fun open(data: Any?) {
      inputValue.value = data
   }

   override fun focus() {
      if (!root.isFocusWithin)
         tas.firstOrNull().ifNotNull { it.requestFocus() } ?: super.focus()
   }

   override fun close() {
      applierHidePopup()
      super.close()
   }

   private fun applierShowPopup() {
      if (applier==null)
         applier = Applier().apply {
            fillActs(taIn.transforms.typeIn)
         }

      if (applierPopup==null)
         applierPopup = showFloating(
            "Use data...",
            WINDOW_ACTIVE(CENTER)
         ) {
            applier!!.root
         }.apply {
            onHidden += { applierPopup = null }
         }
   }

   private fun applierHidePopup() {
      applierPopup?.hideImmediately()
      applierPopup = null
      applier = null
   }

   /* Generates unique name in format 'CustomN', where N is integer. */
   fun generateTaName(): String =
      "Custom" + findFirstInt(1) { i -> tas.none { it.name.value=="Custom$i" } }

   private inner class EditAreaMain: EditArea("In") {
      override fun fillActionData() = applier?.fillActs(transforms.typeIn).toUnit()
   }

   private inner open class EditArea(name: String = generateTaName()): ListAreaNode() {
      var nameL = Label(name)
      val name = nameL.textProperty()
      val isMain = this is EditAreaMain

      init {
         transforms.isHeaderVisible = true
         textArea.prefColumnCount = 80

         // graphics
         var typeL = Label("")
         var typeLUpdater = {
            fun Any?.ertUi() = estimateRuntimeType<Any>().toUi()
            typeL.text = input.size.toString() + "x " + input.ertUi() + " → " + output.size + "x " + output.ertUi()
         }
         input.onChangeAndNow(typeLUpdater)
         output.onChangeAndNow(typeLUpdater)

         var caretL = Label("Caret: 0:0:0")
         textArea.focused sync { caretL.isVisible = it }
         textArea.caretPositionProperty() attach { i ->
            var lastCharAt = 0
            var lastNewlineAt = 0
            var xy = i.toInt() + 1
            var y = 1 + textArea.text.chars32()
               .take(i.toInt())
               .onEach { lastCharAt++ }
               .filter { it.value=='\n'.code || it.value=='\r'.code }
               .onEach { lastNewlineAt = lastCharAt }
               .count()
            var x = xy - lastNewlineAt
            caretL.text = "Pos: $xy:$x:$y"
         }

         var dataI = Icon(IconOC.DATABASE)
            .tooltip("""Set input\n\nSet input for this area. The actual input, its transformation and the output will be discarded.${if (isMain) "\n\nThis edit area is main, so the new input data will update the available actions." else ""}""")
            .action { THIS ->
                  ContextMenu(
                     menuItem("Set input data to empty", null) {
                        setInput(listOf(Unit))
                     },
                     menuItem("Set input data to output data", null) {
                        setInput(output)
                     },
                     menuItem("Set input data to lines of visible text", null) {
                        setInput(valueAsText.split("\\n".toRegex()).dropLastWhile({ it.isEmpty() }))
                     },
                     menuItem("Set input data to system clipboard", null) {
                        setInput(listOf(Clipboard.getSystemClipboard().getAny()))
                     }
                  ).show(THIS, Side.BOTTOM, 0.0, 0.0)
               }
         var remI = Icon(IconFA.MINUS)
            .tooltip("Remove\n\nRemove this edit area.")
            .action { _ -> tas.remove(this) }
         var addI = Icon(IconFA.PLUS)
            .tooltip("Add\n\nCreate new edit area")
            .action { THIS ->
               ContextMenu(
                  menuItem("With no data", null) { createNewAreaWithNoData() },
                  menuItem("With input of this area", null) { createNewAreaWithInputData() },
                  menuItem("With output of this area", null) { createNewAreaWithOutputNoData() }
               ).show(THIS, Side.BOTTOM, 0.0, 0.0)
            }

         // layout
         getNode().children.add(
            0,
            vBox(5.emScaled, CENTER) {
               lay += borderPane {
                  minWidth = 0.0
                  center = nameL
                  right = hBox(5.emScaled, CENTER_RIGHT) {
                     lay += listOf(dataI, Label(), remI, addI)
                  }
               }
               lay += borderPane {
                  minWidth = 0.0
                  left = typeL.apply { padding = Insets(5.emScaled) }
                  right = caretL.apply { padding = Insets(5.emScaled) }
               }
            }
         )

         remI.isDisable = isMain // disallow removing main edit area

         // drag & drop (main area is handled globally)
         if (!isMain) {
            getNode().installDrag(
               IconOC.DATABASE, { "Set data to " + this.name.value + " edit area" },
               { true },
               { setInput(unpackData(it.dragboard.getAny())) }
            )
         }

         textArea.onEventUp(KEY_PRESSED) { e ->
            if (e.code==KeyCode.V && e.isShortcutDown) {
               var pasted_text = Clipboard.getSystemClipboard().string
               if (pasted_text!=null) {
                  var areaLines = textArea.text.split("\\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                  var pastedLines = pasted_text.split("\\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                  var isSelectedAll = textArea.selection==IndexRange(0, textArea.length)
                  if (!isSelectedAll && areaLines.size>1 && areaLines.size==pastedLines.size) {
                     textArea.text = (areaLines zip pastedLines).joinToString("\n") { (a, b) -> a + b }
                     e.consume()
                  } else if (transforms.typeIn==type<Unit>() && transforms.length()==0) {
                     setInput(unpackData(pasted_text))
                     e.consume()
                  } else {
                     textArea.paste()
                     e.consume()
                  }
               }
            }
         }

         setData(name, listOf(Unit))
      }

      fun requestFocus() = textArea.requestFocus()

      // Weird reasons for needing this method, just call it bad design. Not worth 'fixing'.
      fun setData(name: String, input: List<*>) {
         this.name.set(name.capitalLower())
         setInput(input)
      }

      public override fun setInput(data: List<*>) {
         super.setInput(data)
         fillActionData()
      }

      open fun fillActionData() = Unit

      override fun toString() = name.value

      fun createNewAreaWithNoData() =
         tas.add(tas.indexOf(this) + 1, EditArea())

      fun createNewAreaWithInputData() {
         var t = EditArea()
         t.setInput(input)
         tas.add(tas.indexOf(this) + 1, t)
      }

      fun createNewAreaWithOutputNoData() {
         var t = EditArea()
         t.setInput(output)
         tas.add(tas.indexOf(this) + 1, t)
      }
   }

   private inner class Applier {
      private val actCB = SpitComboBox<Act<*>>({ it.name }, textNoVal)
      var ins: Ins? = null
      private val runB = Icon(IconFA.PLAY_CIRCLE, 20.0).onClickDo {
         var action = actCB.value.asIs<Act<Any?>?>()
         if (action==null) return@onClickDo
         if (action.isPartial) {
            var empty = source.isEmpty() || ins!!.values().count()==0
            if (empty) return@onClickDo
            var isTypeAssignable = source.all { action.type.isInstance(it) }
            if (!isTypeAssignable) return@onClickDo
            var isSameInputLength = tas.distinctBy { it.value.size }.size==1
            if (!isSameInputLength) return@onClickDo

            var insValByName = ins!!.values().associate { it.name to it.ta.value }
            val progress = AppProgress.start("${widget.customName.value} - ${action.name}")
            launch(FX) {
               runTry {
                  for (i in source.indices)
                     action.actionPartial!!(
                        source.get(i),
                        insValByName.mapValues { it.value.get(i) }
                     )
                  progress.reportDone(ResultOk(Unit))
               }.ifError {
                  logger.error(it) { "Failed to run ${action.name}" }
                  progress.reportDone(ResultFail(it))
               }
            }
         } else {
            val progress = AppProgress.start("${widget.customName.value} - ${action.name}")
            var insValByName = ins!!.values().associate { it.name to it.ta.value }
            launch(FX) {
               runTry {
                  action.actionImpartial!!(insValByName)
                  progress.reportDone(ResultOk(Unit))
               }.ifError {
                  logger.error(it) { "Failed to run ${action.name}" }
                  progress.reportDone(ResultFail(it))
               }
            }
         }
      }
      var root: VBox = vBox(10.emScaled) {
         lay += label("Action") {
            styleClass += "form-config-pane-config-name"
         }
         lay += textFlow() {
            styleClass += Css.DESCRIPTION
            styleClass += "form-config-pane-config-description"
            lay += text("Action that uses the data in text area")
         }
         lay += actCB
         lay += label("Data") {
            styleClass += "form-config-pane-config-name"
         }
         lay += textFlow() {
            styleClass += Css.DESCRIPTION
            styleClass += "form-config-pane-config-description"
            lay += text("Map text areas to the action input.\nAction can have multiple inputs")
         }
         lay += runB.withText(Side.RIGHT, "Run")
      }

      init {
         root.styleClass += "form-config-pane"
         root.pseudoClassChanged(ConfigPane.Layout.EXTENSIVE.name.lowercase(), true)
         actCB.valueProperty() attach {
            if (it==null) return@attach
            if (ins!=null) root.children.remove(ins!!.node())
            if (root.children.size==7) root.children.removeAt(5)
            ins = Ins(it, it.isInputsFixedLength)
            root.children.add(5, ins!!.node())
            it.node().ifNotNull { root.children.add(6, it) }
         }
      }

      fun fillActs(c: VType<*>) {
         actCB.items setTo acts.getElementsOfSuperV(c.type.raw).toList()
         actCB.value = actCB.items.firstOrNull()
      }
   }

   private open class Act<Y> {
      var name: String
      var max: Int
      var names: List<String?>?
      var type: KClass<Y & Any>
      var isInputsFixedLength: Boolean
      var actionPartial: (suspend (Y, Map<String?, String>) -> Unit)? = null
      var actionImpartial: (suspend (Map<String?, List<String>>) -> Unit)? = null

      constructor(name: String, type: KClass<Y & Any>, max: Int, action: (suspend (Y, Map<String?, String>) -> Unit)?) {
         this.name = name
         this.names = null
         this.type = type
         this.max = max
         this.actionPartial = action
         this.isInputsFixedLength = false
      }

      constructor(name: String, type: KClass<Y & Any>, max: Int, names: List<String?>?, action: (suspend (Y, Map<String?, String>) -> Unit)?): this(name, type, max, action) {
         this.names = names
         this.isInputsFixedLength = true
      }

      open fun node(): Node? = null

      val isPartial: Boolean
         get() = actionPartial!=null
   }

   private class WriteFileAct: Act<Unit>("Write file", Unit::class, 1, listOf("Contents"), null) {
      var nam = v("new_file.txt")
      var loc = v(APP.location)

      init {
         actionImpartial = { data ->
            var file = File(loc.value, nam.value)
            var text = data.get("Contents").orEmpty().joinToString("\n")
            IO {
               file.writeText(text)
            }
         }
      }

      override fun node() =
         vBox(10.emScaled, Pos.TOP_LEFT) {
            styleClass += "form-config-pane"

            lay += label("Output file name") {
               styleClass += "form-config-pane-config-name"
            }
            lay += textFlow() {
               styleClass += Css.DESCRIPTION
               styleClass += "form-config-pane-config-description"
               lay += text("Output file name. Previous file will be overwritten.")
            }
            lay += create(forProperty(String::class.java, "File name", nam)).buildNode()
            lay += label("Output location") {
               styleClass += "form-config-pane-config-name"
            }
            lay += textFlow() {
               styleClass += Css.DESCRIPTION
               styleClass += "form-config-pane-config-description"
               lay += text("Location the file will be saved to")
            }
            lay += create(forProperty(File::class.java, "Location", loc)).buildNode()
         }
   }

   private class ActCreateDirs: Act<Unit>("Create directories", Unit::class, 1, listOf("Names (Paths)"), null) {
      var use_loc = v(false)
      var loc = v(APP.locationHome)

      init {
         actionImpartial = { data ->
            var dir = loc.value
            var names = data.get("Names (Paths)").orEmpty()
            IO {
               names.forEach { name: String ->
                  var d =
                     if (use_loc.value) File(dir, filenamizeString(if (name.startsWith(File.separator)) name.substring(1) else name))
                     else File(name)
                  Files.createDirectories(d.toPath())
               }
            }
         }
      }

      override fun node() =
         vBox(10.emScaled, Pos.TOP_LEFT) {
            styleClass += "form-config-pane"

            lay += label("Relative") {
               styleClass += "form-config-pane-config-name"
            }
            lay += text("If relative, the path will be resolved against the below directory") {
               styleClass += Css.DESCRIPTION
               styleClass += "form-config-pane-config-description"
            }
            lay += create(forProperty(Boolean::class.java, "In directory", use_loc))
               .buildNode()
            lay += label("Relative against") {
               styleClass += "form-config-pane-config-name"
            }
            lay += text("Location the directories will be created in") {
               styleClass += Css.DESCRIPTION
               styleClass += "form-config-pane-config-description"
            }
            lay += create(forProperty(File::class.java, "Location", loc)).buildNode().apply {
               use_loc sync { isDisable = !it }
            }
         }
   }

   private class In(var name: String?, var ta: EditArea)

   private inner class InPane(actions: Collection<String?>?): ValueNode<In?>(null) {
      val name = vn(actions?.firstOrNull())
      val input = v(tas.firstOrNull { it.name.value.equals("out", ignoreCase = true) } ?: taIn)
      val configEditorA = create(forProperty<String?>(String::class.java, "", name).addConstraints(ValueSealedSet({ actions.orEmpty() }), PreserveOrder))
      val configEditorB = create(forProperty(EditArea::class.java, "", input).addConstraints(ValueSealedSet({ tas }), PreserveOrder))
      val root: HBox = hBox(10.emScaled, CENTER_LEFT) {
         lay += configEditorA.buildNode()
         lay += Label("←")
         lay += configEditorB.buildNode()
      }

      override var value: In?
         get() =
            In(configEditorA.config.value, configEditorB.config.value)
         set(value) {
            super.value = value
         }

      public override fun getNode(): Node = root
   }

   private inner class Ins(a: Act<*>, isFixedLength: Boolean) {
      var ins = ListChainValueNode<In, InPane>() { InPane(a.names) }

      init {
         ins.editable.value = !isFixedLength
         ins.growTo1()
         ins.maxChainLength.value = a.max
      }

      fun node(): VBox = ins.getNode()

      fun values(): Sequence<In> = ins.values.asSequence()
   }

   companion object: WidgetCompanion {
      override val name = CONVERTER_NAME
      override val description =
         "Data transformer with transformation chains and data processing, such as text, file or audio tagging."
      override val descriptionLong = """
         User can put text in an edit area and apply transformations on it using available functions. The transformations are applied on each line separately. It is possible to manually edit the text to fine-tune the result.
         This is useful to edit multiple texts in the same way, e.g., to edit filenames or song names. This is done using an 'applier' that uses the 'output' and applies it no an input.
         Input is a list of objects and can be set by drag&drop. It can then be transformed to text (object per line) or other objects using available functions, e.g., song to artist or file to filename.
         Output is the final contents of the text area exactly as visible. Some transformations can produce multiple outputs (again, per line), which produces separate text area for each output. To discern different outputs, text areas have a name in their header.
         Action is determined by the type of input. User can select which output he wants to use. The action then applies the text to the input, each line to its respective object (determined by order). Number of objects and lines must match.
         """.trimIndent()
      override val icon = IconMD.SWAP_HORIZONTAL
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2015)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(AUDIO, UTILITY)
      override val summaryActions = listOf(
         Entry(CONVERTER_NAME, "Sets data as input", "Drag & drop data"),
      )

      private val logger = KotlinLogging.logger { }
      private fun unpackData(o: Any?): List<*> =
         when (o) {
            is String -> split(o, "\n", { it })
            is Collection<*> -> o.toList().materialize()
            else -> listOf(o)
         }
   }
}