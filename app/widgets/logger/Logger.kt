package logger

import javafx.scene.control.TextArea
import javafx.scene.input.KeyCode
import javafx.scene.paint.Color
import javafx.scene.text.Font
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.feature.TextDisplayFeature
import sp.it.pl.main.APP
import sp.it.pl.main.Widgets
import sp.it.pl.main.configure
import sp.it.pl.main.emScaled
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.c
import sp.it.util.conf.cn
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.on
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.x
import java.io.File
import javafx.scene.input.KeyCode as Key

@Widget.Info(
   author = "Martin Polakovic",
   name = Widgets.LOGGER_NAME,
   description = "Displays console output by listening to System.out, which contains application logging.",
   version = "1.0.0",
   year = "2015",
   group = Widget.Group.DEVELOPMENT
)
class Logger(widget: Widget): SimpleController(widget), TextDisplayFeature {

   private val wrapText by cv(false).def(name = "Wrap text", info = "Wrap text at the end of the text area to the next line.")
   private val area = TextArea()

   init {
      root.prefSize = 500.emScaled x 500.emScaled
      root.consumeScrolling()

      root.lay += area.apply {
         isEditable = false
         isWrapText = false
         wrapTextProperty() syncFrom wrapText on onClose

         text = "# This is redirected output (System.out) stream of this application.\n"
      }
      root.lay += Icon().onClickDo {
         @Suppress("RemoveExplicitTypeArguments")
         object: ConfigurableBase<Any?>() {
            val `Boolean` by cv<Boolean>(true)
            var `Boolean_observe` by c<Boolean>(true)
            val `Boolean_null` by cvn<Boolean>(null)
            var `Boolean_null_observe` by cn<Boolean>(null)
            val `KeyCode` by cv<KeyCode>(Key.A)
            var `KeyCode_observe` by c<KeyCode>(Key.A)
            val `KeyCode_null` by cvn<KeyCode>(null)
            var `KeyCode_null_observe` by cn<KeyCode>(null)
            val `File_null` by cvn<File>(null)
            var `File_null_observe` by cn<File>(null)
            val `Font_null` by cvn<Font>(null)
            var `Font_null_observe` by cn<Font>(null)
            val `Color_null` by cvn<Color>(null)
            var `Color_null_observe` by cn<Color>(null)
         }.configure("Test form properties") {}
      }

      APP.systemout.addListener { area.appendText(it) } on onClose
   }

   override fun showText(text: String) = println(text)

}