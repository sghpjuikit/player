package logger

import javafx.event.EventHandler
import javafx.geometry.Pos.TOP_LEFT
import javafx.scene.control.TextArea
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.Priority.ALWAYS
import mu.KLogging
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.layout.feature.TextDisplayFeature
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.IconOC
import sp.it.pl.main.WidgetTags.DEVELOPMENT
import sp.it.pl.main.WidgetTags.UTILITY
import sp.it.pl.main.Widgets.LOGGER_NAME
import sp.it.pl.main.emScaled
import sp.it.pl.ui.objects.icon.CheckIcon
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.async.runLater
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.getDelegateConfig
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.attach
import sp.it.util.reactive.attach1IfNonNull
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.syncNonNullWhile
import sp.it.util.system.open
import sp.it.util.ui.appendTextSmart
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.version
import sp.it.util.units.year

class Logger(widget: Widget): SimpleController(widget), TextDisplayFeature {

   private val area = TextArea()
   private var customText = false
   private val wrapText by cv(false, { area.wrapTextProperty().apply { value = it } }).def(name = "Wrap text", info = "Wrap text at the end of the text area to the next line.")
   private val stdoutReader = Subscribed {
      area.text = APP.systemOut.text()
      area.scrollTop = Double.MAX_VALUE
      APP.systemOut.addListener { area.appendTextSmart(it) }
   }

   init {
      root.prefSize = 500.emScaled x 500.emScaled
      root.consumeScrolling()
      root.lay += hBox(0.0, TOP_LEFT) {
         lay += vBox {
            lay += CheckIcon(wrapText).icons(IconMA.WRAP_TEXT).tooltip(::wrapText.getDelegateConfig().nameUi)
            lay += Icon(IconOC.TRASHCAN).tooltip("Clear").onClickDo { area.clear() }
            lay += Icon(IconFA.FOLDER).tooltip("Log directory").onClickDo { APP.logging.loggingOutputDir.open() }
         }
         lay(ALWAYS) += area.apply {
            isEditable = false
            text = "# This is redirected output (System.out) stream of this application.\n"
            onEventDown(KEY_PRESSED, ENTER) { appendText("\n") }
         }
      }

      onClose += area.sceneProperty().attach1IfNonNull {
         if (!customText) stdoutReader.subscribe()
         if (!customText) runLater { area.scrollTop = Double.MAX_VALUE }
         Subscription(stdoutReader::unsubscribe)
      } on onClose
   }

   override fun showText(text: String) {
      customText = true
      stdoutReader.unsubscribe()
      area.text = text
   }

   companion object: WidgetCompanion, KLogging() {
      override val name = LOGGER_NAME
      override val description = "Displays text or application standard output (stdout), which contains application logging."
      override val descriptionLong = "$description."
      override val icon = IconOC.TERMINAL
      override val version = version(1, 1, 0)
      override val isSupported = true
      override val year = year(2015)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(UTILITY, DEVELOPMENT)
      override val summaryActions = listOf<ShortcutPane.Entry>()
   }
}