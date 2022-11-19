package sp.it.pl.ui.objects.autocomplete

import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.OverrunStyle
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode.A
import javafx.scene.input.KeyCode.BACK_SPACE
import javafx.scene.input.KeyCode.CONTROL
import javafx.scene.input.KeyCode.DELETE
import javafx.scene.input.KeyCode.DOWN
import javafx.scene.input.KeyCode.END
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.KeyCode.HOME
import javafx.scene.input.KeyCode.LEFT
import javafx.scene.input.KeyCode.RIGHT
import javafx.scene.input.KeyCode.TAB
import javafx.scene.input.KeyCode.UP
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.text.TextAlignment
import sp.it.pl.main.Css
import sp.it.pl.main.IconFA
import sp.it.pl.main.appTooltip
import sp.it.pl.main.emScaled
import sp.it.pl.ui.item_node.ConfigEditor
import sp.it.pl.ui.objects.autocomplete.ConfigSearch.Entry
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.access.minus
import sp.it.util.action.Action
import sp.it.util.collections.setToOne
import sp.it.util.conf.Config
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.recurseDF
import sp.it.util.reactive.attach
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.syncFrom
import sp.it.util.text.keysUi
import sp.it.util.type.isSubclassOf
import sp.it.util.ui.hBox
import sp.it.util.ui.install
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.lookupChildAt
import sp.it.util.ui.setMinPrefMaxSize
import sp.it.util.ui.stackPane
import sp.it.util.ui.uninstall

class ConfigSearch: AutoCompletion<Entry> {
   private val textField: TextField
   private val history: History
   private var ignoreEvent = false

   constructor(textField: TextField, history: History = History(), sources: (String) -> Collection<Entry>): super(
      textField,
      sources,
      defaultStringConverter()
   ) {
      this.textField = textField
      this.textField.prefWidth = 550.0.emScaled // affects the popup width
      this.history = history

      this.textField.onEventUp(KEY_PRESSED, CONTROL, UP) { history.up(this) }
      this.textField.onEventUp(KEY_PRESSED, CONTROL, DOWN) { history.down(this) }
   }

   override fun buildPopup() = object: AutoCompletePopup<Entry>() {

      override fun createDefaultSkin(): AutoCompletePopupSkin<Entry> {
         return object: AutoCompletePopupSkin<Entry>(this, 2) {

            init {
               // set keys & allow typing
               node.isFocusTraversable = true
               skinnable.onEventUp(KEY_PRESSED) { e ->
                  if (!ignoreEvent && (e.source==node || e.source==skinnable))
                     if (e.isShortcutDown && (e.code==UP || e.code==DOWN)) {
                        when (e.code) {
                           UP -> history.up(this@ConfigSearch)
                           DOWN -> history.down(this@ConfigSearch)
                           else -> Unit
                        }
                        e.consume()
                     } else if (e.code==ENTER) {
                        chooseSuggestion()
                        e.consume()
                     } else if (e.code==ESCAPE) {
                        if (skinnable.isHideOnEscape) skinnable.hide()
                        e.consume()
                     } else if (e.code==BACK_SPACE) {
                        textField.deletePreviousChar()
                        e.consume()
                     } else if (e.code==DELETE) {
                        textField.deleteNextChar()
                        e.consume()
                     } else if (!e.code.isNavigationKey) {
                        // We re-fire event on text field, so we can type even though it
                        // does not have focus. This causes event stack overflow, so we
                        // defend with a flag.
                        if (!textField.isFocused) {
                           ignoreEvent = true
                           completionTarget.fireEvent(e)
                        }
                     }
                  ignoreEvent = false
               }
               node.onEventDown(KEY_PRESSED) { e ->
                  if (!ignoreEvent && e.source==node) {
                     if (e.isShortcutDown && (e.code==UP || e.code==DOWN)) {
                        when (e.code) {
                           UP -> history.up(this@ConfigSearch)
                           DOWN -> history.down(this@ConfigSearch)
                           else -> Unit
                        }
                        e.consume()
                     } else if (e.isShortcutDown && e.code==A) {
                        textField.selectAll()
                        e.consume()
                     } else if (e.code==END) {
                        if (e.isShiftDown) textField.selectEnd() else textField.positionCaret(textField.length)
                        e.consume()
                     } else if (e.code==HOME) {
                        if (e.isShiftDown) textField.selectHome() else textField.positionCaret(0)
                        e.consume()
                     } else if (e.code==LEFT) {
                        if (e.isShortcutDown) textField.selectPreviousWord() else textField.selectBackward()
                        if (!e.isShiftDown) textField.deselect()
                        e.consume()
                     } else if (e.code==RIGHT) {
                        if (e.isShortcutDown) textField.selectNextWord() else textField.selectForward()
                        if (!e.isShiftDown) textField.deselect()
                        e.consume()
                     } else if (e.code==TAB) {
                        fun recurseGraphics() = node.selectionModel.selectedItem?.graphics?.recurseDF { if (it is Pane) it.children else listOf() }
                        fun firstFocusableGraphics() = recurseGraphics()?.firstOrNull { it.isFocusTraversable }
                        fun lastFocusableGraphics() = recurseGraphics()?.lastOrNull { it.isFocusTraversable }

                        // improves TAB experience by focusing the graphics on the selected row instead of the 1st row
                        if (node.isFocused) {
                           if (e.isShiftDown) lastFocusableGraphics()?.requestFocus()
                           else firstFocusableGraphics()?.requestFocus()
                           e.consume()
                        } else {
                           if (e.isShiftDown) {
                              firstFocusableGraphics()?.takeIf { it.isFocused }.ifNotNull {
                                 node.requestFocus()
                                 e.consume()
                              }
                           } else {
                              lastFocusableGraphics()?.takeIf { it.isFocused }.ifNotNull {
                                 node.requestFocus()
                                 e.consume()
                              }
                           }
                        }
                     }
                  }
                  ignoreEvent = false
               }
            }

            override fun buildListCell(listView: ListView<Entry>) = EntryListCell()
            override fun chooseSuggestionInput(): String = textField.text
         }
      }
   }

   override fun Ctx.acceptSuggestion(suggestion: Entry) {
      suggestion.apply { this@acceptSuggestion.run() }
      history.add(term)
   }

   class History {
      private var historyIndex = 0
      private val history = ArrayList<String>()

      fun up(search: ConfigSearch) {
         if (history.isEmpty()) return
         historyIndex = if (historyIndex==0) history.size - 1 else historyIndex - 1
         search.completionTargetTyped.text = history[historyIndex]
      }

      fun down(search: ConfigSearch) {
         if (history.isEmpty()) return
         historyIndex = if (historyIndex==history.size - 1) 0 else historyIndex + 1
         search.completionTargetTyped.text = history[historyIndex]
      }

      fun add(term: String) {
         val isDiff = history.isEmpty() || !history.last().equals(term, ignoreCase = true)
         if (isDiff) {
            history += term
            historyIndex = history.size - 1
         }
      }
   }

   @Suppress("NonAsciiCharacters", "ClassName")
   interface Entry {
      val name: String
      val icon: GlyphIcons?
      val info: String get() = name
      val graphics: Node? get() = null
      fun Ctx.run()

      class ΛEntry(
         override val name: String,
         override val icon: GlyphIcons?,
         val infoΛ: () -> String,
         override val graphics: Node? = null,
         private val runΛ: Ctx.() -> Unit
      ): Entry {
         override val info get() = infoΛ()
         override fun Ctx.run() = runΛ()
      }

      class SimpleEntry constructor(override val name: String, override val icon: GlyphIcons?, val infoΛ: () -> String, private val runΛ: Ctx.() -> Unit): Entry {
         override val info get() = infoΛ()
         override fun Ctx.run() = runΛ()
      }

      class ConfigEntry constructor(private val config: Config<*>): Entry {
         override val name = "${if (config is Runnable) "Run " else ""}${config.group}.${config.nameUi}"
         override val icon = if (config is Action) IconFA.PLAY else IconFA.COGS
         override val info by lazy { "$name\n\n${config.info}" }
         override val graphics by lazy {
            when {
               config is Action && config.hasKeysAssigned() -> {
                  label(config.keysUi()) {
                     styleClass += Css.DESCRIPTION
                     textAlignment = TextAlignment.RIGHT
                  }
               }
               config.type.isSubclassOf<Boolean>() || config.isEnumerable -> ConfigEditor.create(config).editor
               else -> null
            }
         }

         @Suppress("UNCHECKED_CAST")
         override fun Ctx.run() {
            val value = config.value
            when {
               config is Runnable -> config.run()
               value is Runnable -> value.run()
               value is Boolean -> (config as Config<Boolean?>).value = !value
            }
         }
      }

      companion object {

         fun of(name: String, icon: GlyphIcons?, infoΛ: () -> String = { "" }, graphics: Node? = null, run: Ctx.() -> Unit) = ΛEntry(name, icon, infoΛ, graphics, run)

         fun of(config: Config<*>) = ConfigEntry(config)

      }
   }

   private class EntryListCell: ListCell<Entry>() {
      private val icon = Icon()
      private val text = Label()
      private val configNodeRoot = stackPane()
      private val root = stackPane {
         padding = Insets(0.0, 10.0, 0.0, 10.0)
         lay(CENTER_LEFT) += hBox(5.0, CENTER_LEFT) {
            lay += icon
            lay += text
         }
         lay(CENTER_RIGHT) += configNodeRoot
      }
      private val rootTooltip = appTooltip()

      init {
         icon.isFocusTraversable = false
         text.textAlignment = TextAlignment.LEFT
         text.textOverrun = OverrunStyle.CENTER_ELLIPSIS
         text.setMinPrefMaxSize(USE_COMPUTED_SIZE)
         root.lookupChildAt<HBox>(0).minWidth = 200.0
         root.lookupChildAt<HBox>(0).prefWidthProperty() syncFrom (root.widthProperty() - configNodeRoot.widthProperty() - 10)
         root.lookupChildAt<HBox>(0).maxWidthProperty() syncFrom (root.widthProperty() - 100)
         root.lookupChildAt<HBox>(0).padding = Insets(2.5, 0.0, 2.5, 0.0)
         rootTooltip.textProperty() attach {
            if (it.isNullOrBlank()) root uninstall rootTooltip
            else root install rootTooltip
         }
      }

      override fun updateSelected(selected: Boolean) {
         super.updateSelected(selected)
         icon.select(selected)
      }

      override fun updateItem(item: Entry?, empty: Boolean) {
         if (this.item == item && item!=null) return

         super.updateItem(item, empty)

         if (empty || item==null) {
            text.text = ""
            icon.isVisible = false
            graphic = null
         } else {
            graphic = root
            text.text = item.name
            icon.isVisible = item.icon!=null
            icon.icon(item.icon)
            rootTooltip.text = item.info

            val node = item.graphics
            if (node is HBox) node.alignment = CENTER_RIGHT
            if (node==null) {
               configNodeRoot.children.clear()
            } else {
               configNodeRoot.children setToOne node
               node.onEventDown(KeyEvent.ANY) {
                  when {
                     it.code==ENTER -> Unit // allows executing the action even if item graphics has focus
                     it.code==TAB -> Unit // allows moving focus from item graphics
                     else -> it.consume() // allows interaction with item graphics without interfering with the autocomplete
                  }
               }
               StackPane.setAlignment(node, CENTER_RIGHT)
            }
         }
      }
   }
}