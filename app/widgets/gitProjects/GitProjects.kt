package gitProjects

import java.io.File
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_LEFT
import javafx.scene.control.ScrollPane
import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.main.IconFA
import sp.it.pl.main.WidgetTags.UTILITY
import sp.it.pl.main.emScaled
import sp.it.pl.ui.objects.MdNode
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.async.runIO
import sp.it.util.conf.Constraint.FileActor.DIRECTORY
import sp.it.util.conf.cn
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.conf.noUi
import sp.it.util.conf.only
import sp.it.util.file.children
import sp.it.util.functional.net
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.text.equalsNc
import sp.it.util.ui.hBox
import sp.it.util.ui.hyperlink
import sp.it.util.ui.lay
import sp.it.util.ui.lookupId
import sp.it.util.ui.prefSize
import sp.it.util.ui.scrollPane
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.version
import sp.it.util.units.year

class GitProjects(widget: Widget): SimpleController(widget) {

   val inputFile by cvn<File>(null).only(DIRECTORY).def(name = "Projects root")
   var selection by cn<String>(null).noUi()
   val md = MdNode()

   init {
      root.prefSize = 400.emScaled x 400.emScaled
      root.lay += hBox(10.emScaled, null) {
         lay += stackPane {
            padding = Insets(50.emScaled, 0.0, 50.emScaled, 0.0)
            minWidth = 200.emScaled
            lay += scrollPane {
               id = "projects"
               isFitToWidth = true
               vbarPolicy = AS_NEEDED
               hbarPolicy = AS_NEEDED
            }
         }
         lay += md
      }

      inputFile.sync {
         runIO {
            it?.children().orEmpty().filter { it.isDirectory }.toList()
         } ui {
            root.lookupId<ScrollPane>("projects").content = vBox(0.0, CENTER_LEFT) {
               lay += it.map { f ->
                  hyperlink(f.name) {
                     onEventDown(MOUSE_CLICKED, PRIMARY) { select(f) }
                  }
               }
            }
            it.find { it.name==selection }?.net(::select)
         }
      } on onClose
   }

   fun select(project: File) {
      selection = project.name
      md.readFile(project.children().find { it.name.equalsNc("README.md") })
   }

   companion object: WidgetCompanion {
      override val name = "Git projects"
      override val description = "Displays git projects and displays README.md"
      override val descriptionLong = "$description."
      override val icon = IconFA.GIT
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2021)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(UTILITY)
      override val summaryActions = listOf<ShortcutPane.Entry>()
   }
}