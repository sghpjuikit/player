package sp.it.pl.layout

import javafx.geometry.Orientation.VERTICAL
import sp.it.pl.layout.controller.ControllerIntro
import sp.it.pl.layout.controller.ControllerEmpty
import sp.it.pl.layout.controller.ControllerNode
import sp.it.pl.main.APP
import sp.it.pl.main.Widgets.PLAYBACK
import sp.it.pl.main.Widgets.PLAYLIST
import sp.it.util.file.div

/** [ControllerEmpty] factory */
val emptyWidgetFactory = WidgetFactory("Empty", ControllerEmpty::class, APP.location.widgets/"empty")

/** [ControllerIntro] factory */
val introWidgetFactory = WidgetFactory("Intro", ControllerIntro::class, APP.location.widgets/"intro")

/** [ControllerNode] factory */
val nodeWidgetFactory = WidgetFactory("Node", ControllerNode::class, APP.location.widgets/"node")

/** [ContainerBi] { [PLAYBACK] + [PLAYLIST] } factory */
val initialTemplateFactory = TemplateFactory("Playback + Playlist") {
   ContainerBi(VERTICAL).apply {
      children += 1 to APP.widgetManager.factories.getFactory(PLAYBACK.id).orNone().create()
      children += 2 to APP.widgetManager.factories.getFactory(PLAYLIST.id).orNone().create()
   }
}