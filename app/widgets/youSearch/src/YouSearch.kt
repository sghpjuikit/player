package youSearch

import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import javafx.geometry.Pos.CENTER
import javafx.geometry.Side.RIGHT
import mu.KLogging
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconUN
import sp.it.pl.main.WidgetTags.UTILITY
import sp.it.pl.main.emScaled
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.system.browse
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.textArea
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.uri
import sp.it.util.units.version
import sp.it.util.units.year

@Suppress("SpellCheckingInspection")
class YouSearch(widget: Widget): SimpleController(widget) {
   val txtArea = textArea("")

   init {
      root.prefSize = 400.emScaled x 300.emScaled
      root.lay += vBox(5.emScaled, CENTER) {
         lay += txtArea
         lay += Icon(IconFA.PLAY)
            .onClickDo { browse() }
            .withText(RIGHT, CENTER, "Send to You AI")
      }
   }

   override fun focus() = txtArea.requestFocus()

   fun txt() = URLEncoder.encode(txtArea.text.orEmpty(), UTF_8)!!

   fun browse() = uri("https://you.com/search?q=${txt()}&tbm=youchat&cfr=chat").browse()

   companion object: WidgetCompanion, KLogging() {
      override val name = "You Search"
      override val description = "Sends prompts to you.com AI and opens the chat in browser"
      override val descriptionLong = "$description."
      override val icon = IconUN(0x2e2a)
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2023)
      override val author = "spit"
      override val contributor = ""
      override val summaryActions = listOf<Entry>()
      override val tags = setOf(UTILITY)
   }

}