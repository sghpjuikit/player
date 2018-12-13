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
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.text.TextAlignment
import sp.it.pl.gui.itemnode.ConfigField
import sp.it.pl.gui.objects.autocomplete.ConfigSearch.Entry
import sp.it.pl.layout.widget.ComponentFactory
import sp.it.pl.main.APP
import sp.it.pl.main.appTooltip
import sp.it.pl.util.Util.containsNoCase
import sp.it.pl.util.access.minus
import sp.it.pl.util.action.Action
import sp.it.pl.util.conf.Config
import sp.it.pl.util.functional.setToOne
import sp.it.pl.util.graphics.install
import sp.it.pl.util.graphics.lay
import sp.it.pl.util.graphics.setMinPrefMaxSize
import sp.it.pl.util.graphics.stackPane
import sp.it.pl.util.reactive.onEventUp
import sp.it.pl.util.reactive.syncFrom
import java.util.ArrayList

class ConfigSearch: AutoCompletion<Entry> {
    private val textField: TextField
    private val history: History
    private var ignoreEvent = false

    constructor(textField: TextField, history: History = History(), entries: () -> Sequence<Entry>): super(
            textField,
            { text ->
                val phrases = text.split(" ").toList()
                entries()
                        .filter { phrases.all { phrase -> containsNoCase(it.searchText, phrase) } }
                        .sortedBy { it.name }
                        .toList()
            },
            AutoCompletion.defaultStringConverter()
    ) {
        this.textField = textField
        this.history = history
        this.textField.prefWidth = 450.0 // affects the popup width
    }

    override fun buildPopup(): AutoCompletePopup<Entry> {
        return object: AutoCompletePopup<Entry>() {

            override fun createDefaultSkin(): AutoCompletePopupSkin<Entry> {
                return object: AutoCompletePopupSkin<Entry>(this, 2) {

                    init {
                        // set keys & allow typing
                        skinnable.onEventUp(KEY_PRESSED) {
                            if (!ignoreEvent)
                                if (it.isControlDown && (it.code==KeyCode.UP || it.code==KeyCode.DOWN)) {
                                    when (it.code) {
                                        KeyCode.UP -> history.up(this@ConfigSearch)
                                        KeyCode.DOWN -> history.down(this@ConfigSearch)
                                        else -> {}
                                    }
                                } else if (it.code==KeyCode.BACK_SPACE) {
                                    textField.deletePreviousChar()
                                    it.consume()
                                } else if (it.code==KeyCode.DELETE) {
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
                                if (it.isControlDown && (it.code==KeyCode.UP || it.code==KeyCode.DOWN)) {
                                    when (it.code) {
                                        KeyCode.UP -> history.up(this@ConfigSearch)
                                        KeyCode.DOWN -> history.down(this@ConfigSearch)
                                        else -> {}
                                    }
                                    it.consume()
                                } else if (it.isControlDown && it.code==KeyCode.A) {
                                    textField.selectAll()
                                    it.consume()
                                    // TODO:
                                    // else if (e.getCode()==KeyCode.BACK_SPACE) {
                                    //     textField.deletePreviousChar(); // doesn't work here
                                    //     e.consume();
                                } else if (it.code==KeyCode.END) {
                                    if (it.isShiftDown) textField.selectEnd() else textField.positionCaret(textField.length)
                                    it.consume()
                                } else if (it.code==KeyCode.HOME) {
                                    if (it.isShiftDown) textField.selectHome() else textField.positionCaret(0)
                                    it.consume()
                                } else if (it.code==KeyCode.LEFT) {
                                    if (it.isControlDown) textField.selectPreviousWord() else textField.selectBackward()
                                    if (!it.isShiftDown) textField.deselect()
                                    it.consume()
                                } else if (it.code==KeyCode.RIGHT) {
                                    if (it.isControlDown) textField.selectNextWord() else textField.selectForward()
                                    if (!it.isShiftDown) textField.deselect()
                                    it.consume()
                                }
                                // TODO: else if (!e.getCode().isNavigationKey()) {}
                            ignoreEvent = false
                        }
                    }

                    override fun buildListViewCellFactory(listView: ListView<Entry>) = EntryListCell()
                }
            }
        }
    }

    override fun acceptSuggestion(suggestion: Entry) {
        suggestion.run()
        history.add(this)
    }

    class History {
        private var historyIndex = 0
        private val history = ArrayList<String>()

        fun up(search: ConfigSearch) {
            if (history.isEmpty()) return
            historyIndex = if (historyIndex==0) history.size-1 else historyIndex-1
            search.completionTargetTyped.text = history[historyIndex]
        }

        fun down(search: ConfigSearch) {
            if (history.isEmpty()) return
            historyIndex = if (historyIndex==history.size-1) 0 else historyIndex+1
            search.completionTargetTyped.text = history[historyIndex]
        }

        fun add(search: ConfigSearch) {
            val last = if (history.isEmpty()) null else history[history.size-1]
            val curr = search.completionTargetTyped.text
            val isDiff = last!=null && curr!=null && !last.equals(curr, ignoreCase = true)
            if (isDiff) {
                history += curr
                historyIndex = history.size-1
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

        class SimpleEntry constructor(override val name: String, override val info: String, private val runΛ: () -> Unit): Entry {
            override fun run() = runΛ()
        }

        class ConfigEntry constructor(private val config: Config<*>): Entry {
            override val name = "${config.group}.${config.guiName}"
            override val searchText = if (config is Action) name+config.keys else name
            override val info = "$name\n\n${config.info}"
            override val graphics by lazy {
                when {
                    config is Action && config.hasKeysAssigned() -> {
                        Label(config.keys).apply {
                            textAlignment = TextAlignment.RIGHT
                        }
                    }
                    config.type==Boolean::class.java || config.isTypeEnumerable -> ConfigField.create(config).getNode()
                    else -> null
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun run() {
                val value = config.value
                when {
                    config is Runnable -> config.run()
                    value is Runnable -> value.run()
                    value is Boolean -> (config as Config<Boolean?>).setNapplyValue(!value)
                }
            }
        }

        companion object {

            fun of(nameΛ: () -> String, infoΛ: () -> String = { "" }, searchTextΛ: () -> String = nameΛ, graphicsΛ: () -> Node, runΛ: () -> Unit) = Entry.ΛEntry(nameΛ, infoΛ, searchTextΛ, graphicsΛ, runΛ)

            fun of(config: Config<*>) = Entry.ConfigEntry(config)

            fun of(f: ComponentFactory<*>): Entry {
                return SimpleEntry(
                        "Open widget ${f.nameGui()}",
                        "Open widget ${f.nameGui()}\n\nOpens the widget in new window.",
                        { APP.windowManager.launchComponent(f.create()) }
                )
            }
        }
    }

    private class EntryListCell: ListCell<Entry>() {
        private val text = Label()
        private val configNodeRoot = stackPane()
        private val root = stackPane {
            lay(CENTER_LEFT) += text
            lay(CENTER_RIGHT) += configNodeRoot
        }
        private val rootTooltip = appTooltip()

        init {
            text.textAlignment = TextAlignment.LEFT
            text.textOverrun = OverrunStyle.CENTER_ELLIPSIS
            text.setMinPrefMaxSize(USE_COMPUTED_SIZE)
            text.prefWidthProperty() syncFrom root.widthProperty()-configNodeRoot.widthProperty()-10
            text.minWidth = 200.0
            text.maxWidthProperty() syncFrom root.widthProperty()-100
            text.padding = Insets(5.0, 0.0, 0.0, 10.0)
            root install rootTooltip
        }

        override fun updateItem(item: Entry?, empty: Boolean) {
            super.updateItem(item, empty)

            if (empty || item==null) {
                text.text = ""
                graphic = null
            } else {
                graphic = root
                rootTooltip.text = item.info
                text.text = item.name

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