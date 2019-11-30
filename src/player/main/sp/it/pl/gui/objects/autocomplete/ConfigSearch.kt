package sp.it.pl.gui.objects.autocomplete

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
import javafx.scene.input.KeyCode.HOME
import javafx.scene.input.KeyCode.LEFT
import javafx.scene.input.KeyCode.RIGHT
import javafx.scene.input.KeyCode.UP
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.text.TextAlignment
import sp.it.pl.gui.itemnode.ConfigField
import sp.it.pl.gui.objects.autocomplete.ConfigSearch.Entry
import sp.it.pl.main.appTooltip
import sp.it.pl.main.emScaled
import sp.it.util.access.minus
import sp.it.util.action.Action
import sp.it.util.collections.setToOne
import sp.it.util.conf.Config
import sp.it.util.reactive.attach
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.syncFrom
import sp.it.util.type.isSubclassOf
import sp.it.util.ui.install
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.setMinPrefMaxSize
import sp.it.util.ui.stackPane
import sp.it.util.ui.uninstall
import java.util.ArrayList

class ConfigSearch: AutoCompletion<Entry> {
   private val textField: TextField
   private val history: History
   private var ignoreEvent = false

   constructor(textField: TextField, history: History = History(), entries: () -> Sequence<Entry>): super(
      textField,
      { text ->
         if (text.isEmpty())
            listOf()
         else {
            val phrases = text.split(" ").toList()
            entries()
               .filter { phrases.all { phrase -> it.searchText.contains(phrase, true) } }
               .sortedBy { it.name }
               .toList()
         }
      },
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
               skinnable.onEventUp(KEY_PRESSED) {
                  if (!ignoreEvent)
                     if (it.isControlDown && (it.code==UP || it.code==DOWN)) {
                        when (it.code) {
                           UP -> history.up(this@ConfigSearch)
                           DOWN -> history.down(this@ConfigSearch)
                           else -> Unit
                        }
                        it.consume()
                     } else if (it.code==BACK_SPACE) {
                        textField.deletePreviousChar()
                        it.consume()
                     } else if (it.code==DELETE) {
                        textField.deleteNextChar()
                        it.consume()
                     } else if (!it.code.isNavigationKey) {
                        // We re-fire event on text field so we can type even though it
                        // does not have focus. This causes event stack overflow, so we
                        // defend with a flag.
                        if (!textField.isFocused) {
                           ignoreEvent = true
                           completionTarget.fireEvent(it)
                        }
                     }
                  ignoreEvent = false
               }
               node.onEventUp(KEY_PRESSED) {
                  if (!ignoreEvent)
                     if (it.isControlDown && (it.code==UP || it.code==DOWN)) {
                        when (it.code) {
                           UP -> history.up(this@ConfigSearch)
                           DOWN -> history.down(this@ConfigSearch)
                           else -> Unit
                        }
                        it.consume()
                     } else if (it.isControlDown && it.code==A) {
                        textField.selectAll()
                        it.consume()
                        // TODO:
                        // else if (e.getCode()==KeyCode.BACK_SPACE) {
                        //     textField.deletePreviousChar(); // doesn't work here
                        //     e.consume();
                     } else if (it.code==END) {
                        if (it.isShiftDown) textField.selectEnd() else textField.positionCaret(textField.length)
                        it.consume()
                     } else if (it.code==HOME) {
                        if (it.isShiftDown) textField.selectHome() else textField.positionCaret(0)
                        it.consume()
                     } else if (it.code==LEFT) {
                        if (it.isControlDown) textField.selectPreviousWord() else textField.selectBackward()
                        if (!it.isShiftDown) textField.deselect()
                        it.consume()
                     } else if (it.code==RIGHT) {
                        if (it.isControlDown) textField.selectNextWord() else textField.selectForward()
                        if (!it.isShiftDown) textField.deselect()
                        it.consume()
                     }
                  // TODO: else if (!e.getCode().isNavigationKey()) {}
                  ignoreEvent = false
               }
            }

            override fun buildListCell(listView: ListView<Entry>) = EntryListCell()
         }
      }
   }

   override fun acceptSuggestion(suggestion: Entry) {
      suggestion.run()
      history.add(suggestion)
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

      fun add(suggestion: Entry) {
         val curr = suggestion.name
         val isDiff = history.isEmpty() || !history.last().equals(curr, ignoreCase = true)
         if (isDiff) {
            history += curr
            historyIndex = history.size - 1
         }
      }
   }

   @Suppress("NonAsciiCharacters", "ClassName")
   interface Entry: Runnable {
      val name: String
      val searchText: String get() = name
      val info: String get() = name
      val graphics: Node? get() = null

      class ΛEntry(nameΛ: () -> String, infoΛ: () -> String, searchTextΛ: () -> String, graphicsΛ: () -> Node, private val runΛ: () -> Unit): Entry {
         override val name = nameΛ()
         override val info = infoΛ()
         override val searchText = searchTextΛ()
         override val graphics = graphicsΛ()
         override fun run() = runΛ()
      }

      class SimpleEntry constructor(override val name: String, infoΛ: () -> String, private val runΛ: () -> Unit): Entry {
         override fun run() = runΛ()
         override val info = infoΛ()
      }

      class ConfigEntry constructor(private val config: Config<*>): Entry {
         override val name = "${if (config is Runnable) "Run " else ""}${config.group}.${config.guiName}"
         override val searchText = if (config is Action) name + config.keys else name
         override val info by lazy { "$name\n\n${config.info}" }
         override val graphics by lazy {
            when {
               config is Action && config.hasKeysAssigned() -> {
                  label(config.keys) {
                     textAlignment = TextAlignment.RIGHT
                  }
               }
               config.type.isSubclassOf<Boolean>() || config.isTypeEnumerable -> ConfigField.create(config).editor
               else -> null
            }
         }

         @Suppress("UNCHECKED_CAST")
         override fun run() {
            val value = config.value
            when {
               config is Runnable -> config.run()
               value is Runnable -> value.run()
               value is Boolean -> (config as Config<Boolean?>).value = !value
            }
         }
      }

      companion object {

         fun of(name: () -> String, info: () -> String = { "" }, searchText: () -> String = name, graphics: () -> Node, run: () -> Unit) = ΛEntry(name, info, searchText, graphics, run)

         fun of(config: Config<*>) = ConfigEntry(config)

      }
   }

   private class EntryListCell: ListCell<Entry>() {
      private val text = Label()
      private val configNodeRoot = stackPane()
      private val root = stackPane {
         padding = Insets(0.0, 10.0, 0.0, 10.0)
         lay(CENTER_LEFT) += text
         lay(CENTER_RIGHT) += configNodeRoot
      }
      private val rootTooltip = appTooltip()

      init {
         text.textAlignment = TextAlignment.LEFT
         text.textOverrun = OverrunStyle.CENTER_ELLIPSIS
         text.setMinPrefMaxSize(USE_COMPUTED_SIZE)
         text.prefWidthProperty() syncFrom root.widthProperty() - configNodeRoot.widthProperty() - 10
         text.minWidth = 200.0
         text.maxWidthProperty() syncFrom root.widthProperty() - 100
         text.padding = Insets(2.5, 0.0, 2.5, 0.0)
         rootTooltip.textProperty() attach { if (it.isNullOrBlank()) root uninstall rootTooltip else root install rootTooltip }
      }

      override fun updateItem(item: Entry?, empty: Boolean) {
         super.updateItem(item, empty)

         if (empty || item==null) {
            text.text = ""
            graphic = null
         } else {
            graphic = root
            text.text = item.name
            rootTooltip.text = item.info

            val node = item.graphics
            if (node is HBox) node.alignment = CENTER_RIGHT
            if (node==null) {
               configNodeRoot.children.clear()
            } else {
               configNodeRoot.children setToOne node
               StackPane.setAlignment(node, CENTER_RIGHT)
            }
         }
      }
   }
}