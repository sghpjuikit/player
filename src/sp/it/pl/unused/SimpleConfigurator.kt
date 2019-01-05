package sp.it.pl.unused

import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.StackPane
import sp.it.pl.gui.itemnode.ConfigField
import sp.it.pl.gui.itemnode.ConfigField.STYLECLASS_CONFIG_FIELD_WARN_BUTTON
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconOC
import sp.it.pl.util.access.v
import sp.it.pl.util.conf.Configurable
import sp.it.pl.util.functional.Try
import sp.it.pl.util.functional.sortedBy
import sp.it.pl.util.graphics.fxml.ConventionFxmlLoader
import sp.it.pl.util.reactive.onEventDown
import java.util.ArrayList
import java.util.function.Consumer

/**
 * [sp.it.pl.util.conf.Configurable] editor. Has optional submit button.
 *
 * @param <T> type of the [sp.it.pl.util.conf.Configurable] for this component.
 */
class SimpleConfigurator<T>: AnchorPane {

    @FXML private lateinit var fields: GridPane
    @FXML private lateinit var buttonPane: BorderPane
    @FXML private lateinit var okPane: StackPane
    @FXML private lateinit var fieldsPane: ScrollPane
    @FXML private lateinit var warnLabel: Label
    private val okB = Icon(IconOC.CHECK, 25.0)
    private val anchorOk: Double
    private val anchorWarn = 20.0
    private val configFields = ArrayList<ConfigField<T>>()
    /** Configurable object. */
    @JvmField val configurable: Configurable<T>
    /** Invoked when user submits the editing. Default does nothing. */
    @JvmField val onOK: (Configurable<T>) -> Unit
    /** Denotes whether parent window or popup should close immediately after [onOK] executes. Default false. */
    @JvmField val hideOnOk = v(false)
    /** Denotes whether there is action that user can execute. */
    @JvmField val hasAction = v(false)

    @Suppress("UNCHECKED_CAST")
    private constructor(c: Configurable<T>, on_OK: ((Configurable<T>) -> Unit)?): super() {
        configurable = c
        onOK = on_OK ?: {}
        hasAction.set(on_OK!=null)
        ConventionFxmlLoader(this).loadNoEx<Any>()
        okPane.children.add(okB)
        anchorOk = AnchorPane.getBottomAnchor(fieldsPane)
        showOkButton(hasAction.get())
        configFields.clear()
        fields.children.clear()
        configurable.fields.stream()
                .sortedBy { it.guiName }
                .forEach {
                    val cf = ConfigField.create(it)
                    configFields.add(cf)
                    fields.add(cf.createLabel(), 0, configFields.size-1)
                    fields.add(cf.getNode(), 1, configFields.size-1)
                }
        val observer = Consumer<Try<T, String>> { validate() }
        configFields.forEach { it.observer = observer }

        fieldsPane.onEventDown(KEY_PRESSED) {
            if (it.code==ENTER) {
                ok()
                it.consume()
            }
        }
        fieldsPane.onEventDown(SCROLL) { it.consume() }

        validate()
    }

    @FXML
    fun ok() {
        validate().ifOk {
            configFields.forEach { it.apply() }
            if (hasAction.value) onOK.invoke(configurable)
            if (hideOnOk.value && scene?.window?.isShowing==true) scene.window.hide()   // TODO: implement properly
        }
    }

    private fun validate(): Try<*, *> {
        val validation: Try<*, *> = configFields.asSequence()
                .map { it.value }
                .reduce { v1, v2 -> v1.and(v2) } ?: Try.ok<Unit?, Unit?>(null)
        showWarnButton(validation)
        return validation
    }

    fun focusFirstConfigField() = configFields.firstOrNull()?.focus()

    private fun showWarnButton(validation: Try<*, *>) {
        if (validation.isError) okB.styleclass(STYLECLASS_CONFIG_FIELD_WARN_BUTTON)
        okB.icon(if (validation.isOk) IconOC.CHECK else IconFA.EXCLAMATION_TRIANGLE)
        okB.isMouseTransparent = validation.isError
        buttonPane.bottom = if (validation.isOk) null else warnLabel
        updateAnchor()
        warnLabel.text = if (validation.isError) "Form contains wrong data" else ""
    }

    private fun showOkButton(visible: Boolean) {
        buttonPane.isVisible = visible
        updateAnchor()
    }

    private fun updateAnchor() {
        val isOkVisible = buttonPane.isVisible
        val isWarnVisible = buttonPane.bottom!=null
        val a = (if (isOkVisible) anchorOk else 0.0)+if (isWarnVisible) anchorWarn else 0.0
        AnchorPane.setBottomAnchor(fieldsPane, a)
    }

    companion object {
        /**
         * @param configurable configurable object
         * @param onOk on submit action (taking the configurable as input parameter) or null if none. Submit button is
         * only visible if there is an action to execute.
         */
        @Suppress("UNCHECKED_CAST")
        @JvmOverloads
        @JvmStatic
        fun <T, C: Configurable<T>> simpleConfigurator(configurable: C, onOk: ((C) -> Unit)? = null) = SimpleConfigurator(configurable, onOk?.let { { c: Configurable<T> -> onOk(c as C) } })
    }
}