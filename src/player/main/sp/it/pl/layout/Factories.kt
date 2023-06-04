package sp.it.pl.layout

import javafx.geometry.Orientation.VERTICAL
import sp.it.pl.layout.controller.ControllerIntro
import sp.it.pl.layout.controller.ControllerEmpty
import sp.it.pl.main.APP
import sp.it.pl.main.Widgets.PLAYBACK
import sp.it.pl.main.Widgets.PLAYLIST
import sp.it.util.file.div

/** [ControllerEmpty] factory */
val emptyWidgetFactory = WidgetFactory(ControllerEmpty::class, APP.location.widgets/"emptyWidget")

/** [ControllerIntro] factory */
val introWidgetFactory = WidgetFactory(ControllerIntro::class, APP.location.widgets/"introWidget")

/** [ContainerBi] { [PLAYBACK] + [PLAYLIST] } factory */
val initialTemplateFactory = TemplateFactory("Playback + Playlist") {
   ContainerBi(VERTICAL).apply {
      children += 1 to APP.widgetManager.factories.getFactory(PLAYBACK.id).orNone().create()
      children += 2 to APP.widgetManager.factories.getFactory(PLAYLIST.id).orNone().create()
   }
}