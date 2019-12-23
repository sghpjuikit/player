package iconBox

import de.jensd.fx.glyphs.GlyphIcons
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.BUS
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Info
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.feature.HorizontalDock
import sp.it.pl.main.emScaled
import sp.it.util.access.VarAction
import sp.it.util.access.VarEnum
import sp.it.util.collections.setTo
import sp.it.util.conf.Config
import sp.it.util.conf.ListConfigurable
import sp.it.util.conf.cList
import sp.it.util.conf.def
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.onChangeAndNow
import sp.it.util.ui.flowPane
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.x

@Info(
   author = "Martin Polakovic",
   name = "IconBar",
   description = "Provides button with customizable action.",
   howto = "Available actions:\n" + "    Button click : execute action",
   version = "0.9.0",
   year = "2014",
   group = Widget.Group.OTHER
)
class IconBox(widget: Widget): SimpleController(widget), HorizontalDock {

   private val icons by cList(
      { Icon(BUS) },
      { icon ->
         ListConfigurable.heterogeneous(
            Config.forProperty(GlyphIcons::class.java, "Icon", VarEnum(icon.glyph, Icon.GLYPHS).apply { attach { icon.icon(it) } }),
            Config.forProperty(String::class.java, "Action", VarAction(icon.onClickAction) { icon.action(it) })
         )
      }
   ).def(name = "Icons", info = "List of icons to show")

   init {
      root.prefSize = 400.emScaled x 50.emScaled
      root.lay += flowPane(5.0, 5.0) {
         icons.onChangeAndNow { children setTo icons } on onClose
      }
   }

}