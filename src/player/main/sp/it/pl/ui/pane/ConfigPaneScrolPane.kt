package sp.it.pl.ui.pane

import javafx.scene.control.ScrollPane
import sp.it.pl.main.emScaled
import sp.it.pl.ui.pane.ConfigPane.Layout.MINI
import sp.it.util.conf.ListConfigurable
import sp.it.util.conf.getDelegateConfig
import sp.it.util.functional.net
import sp.it.util.ui.prefSize
import sp.it.util.ui.x

class ConfigPaneScrolPane<T>(val configPane: ConfigPane<T> = ConfigPane()): ScrollPane() {
   init {
      isFitToWidth = true
      isFitToHeight = false
      prefSize = -1 x -1
      vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
      hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
      minWidth = 250.emScaled
      content = configPane
   }
}