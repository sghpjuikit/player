package gitProjects

import de.jensd.fx.glyphs.GlyphIcons
import java.io.File
import java.io.File.separatorChar
import java.util.Stack
import javafx.geometry.Insets
import javafx.geometry.Orientation.VERTICAL
import javafx.geometry.Pos.CENTER
import javafx.scene.control.ScrollPane
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.input.KeyCode.BACK_SPACE
import javafx.scene.input.KeyCode.F5
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Priority.ALWAYS
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconOC
import sp.it.pl.main.WidgetTags.DEVELOPMENT
import sp.it.pl.main.WidgetTags.UTILITY
import sp.it.pl.main.contextMenuFor
import sp.it.pl.main.emScaled
import sp.it.pl.main.listBox
import sp.it.pl.main.listBoxRow
import sp.it.pl.ui.objects.MdNode
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.async.runVT
import sp.it.util.collections.setTo
import sp.it.util.conf.Constraint.FileActor.DIRECTORY
import sp.it.util.conf.c
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.conf.noUi
import sp.it.util.conf.only
import sp.it.util.file.children
import sp.it.util.file.hasExtension
import sp.it.util.file.toFileOrNull
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.ifNull
import sp.it.util.functional.net
import sp.it.util.functional.traverse
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.system.open
import sp.it.util.text.capitalLower
import sp.it.util.text.equalsNc
import sp.it.util.text.nameUi
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.lookupId
import sp.it.util.ui.prefSize
import sp.it.util.ui.scrollPane
import sp.it.util.ui.separator
import sp.it.util.ui.show
import sp.it.util.ui.stackPane
import sp.it.util.ui.x
import sp.it.util.units.version
import sp.it.util.units.year

class GitProjects(widget: Widget): SimpleController(widget) {

   val inputFile by cvn<File>(null).only(DIRECTORY).def(name = "Projects root")
   var selection by c("").noUi()
   val projects = mutableListOf<Project>()
   val md = MdNode()
   val mdHistory = Stack<File>()

   init {
      root.prefSize = 400.emScaled x 400.emScaled
      root.onEventDown(KEY_PRESSED, F5) { refreshProjects() }
      root.onEventDown(KEY_PRESSED, BACK_SPACE, false) { if (popFilePossible()) { popFile(); it.consume() } }
      root.onEventDown(MOUSE_CLICKED, SECONDARY, false) { if (popFilePossible()) { popFile(); it.consume() } }
      root.lay += hBox(20.emScaled, CENTER) {
         lay += stackPane {
            padding = Insets(50.emScaled, 0.0, 50.emScaled, 0.0)
            minWidth = 220.emScaled
            lay += scrollPane {
               id = "projects"
               isFitToHeight = true
               isFitToWidth = true
               vbarPolicy = NEVER
               hbarPolicy = NEVER
            }
         }
         lay += separator(VERTICAL) { maxHeight = 200.emScaled }
         lay(ALWAYS) += md.apply {
            uriHandler.value = {
               it.toFileOrNull()?.takeIf { it hasExtension "md" }
                  .ifNotNull(::visitFile)
                  .ifNull(it::open)
            }
         }
      }

      inputFile.sync { refreshProjects(it) } on onClose
   }

   fun refreshProjects(root: File? = inputFile.value) {
      runVT {
         root?.listProjects().orEmpty().map(::Project).toList()
      } ui {
         projects setTo it
         this.root.lookupId<ScrollPane>("projects").content = listBox {
            lay += projects.map { it.row }
         }
         projects.find { it.name==selection }?.select(true)
      }
   }

   fun File.listProjects(depth: Int = 3): Sequence<File> =
      sequence {
         if (depth>=2) yield(this@listProjects)
         if (depth>0) {
            val children = children()
            children.filter { it.isDirectory }.forEach {
               if (children.any { it.name.equalsNc("README.md") }) yield(this@listProjects)
               else if (children.any { it.name == ".git" }) yield(this@listProjects)
               else yieldAll(it.listProjects(depth-1))
            }
         }
      }
      // always include project parent dirs
      .flatMap { it.traverse { it.parentFile }.takeWhile { it != inputFile.value } }
      .distinct()
      .sortedBy { it.path }

   fun visitProject(p: Project?) {
      mdHistory.clear()
      visitFile(p?.readmeFile)
   }

   fun visitFile(f: File?) {
      if (f!=null) mdHistory.push(f)
      md.readFile(f)
   }

   fun popFilePossible(): Boolean = mdHistory.size==1

   fun popFile() {
      if (mdHistory.size<=1) return
      mdHistory.pop()
      md.readFile(mdHistory.peek())
   }

   inner class Project(val dir: File) {
      val name = dir.name.capitalLower()
      val depth = inputFile.value?.net { dir.relativeTo(it).path.count { it==separatorChar } } ?: 0
      val children = dir.children()
      var gitDir = children.find { it.name == ".git" }
      var readmeFile = dir.children().find { it.name.equalsNc("README.md") }
      val glyph: GlyphIcons = when {
         gitDir!=null -> IconFA.GIT
         readmeFile!=null -> IconOC.MARKDOWN
         else -> IconFA.FOLDER
      }
      val row = listBoxRow(glyph, name) {
         padding = Insets(0.0, 0.0, 0.0, (1+depth)*12.emScaled)
         icon.onClickDo(null, null) { _, e ->
            when (e?.button) {
               null, PRIMARY -> this@Project.select(true)
               SECONDARY -> contextMenuFor(dir).show(this, e)
               else -> Unit
            }
         }
      }

      fun select(s: Boolean) {
         row.select(s)
         if (s) projects.find { it!==this && it.name==selection }?.select(false)
         if (s) selection = name
         if (s) visitProject(this)
      }
   }

   companion object: WidgetCompanion {
      override val name = "Git projects"
      override val description = "Displays git projects and displays README.md"
      override val descriptionLong = "$description."
      override val icon = IconFA.GIT
      override val version = version(1, 1, 0)
      override val isSupported = true
      override val year = year(2021)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(UTILITY, DEVELOPMENT)
      override val summaryActions = listOf(
         Entry("Data", "Refresh projects", F5.nameUi),
         Entry("Data", "Back (after visiting link)", BACK_SPACE.nameUi),
         Entry("Data", "Back (after visiting link)", SECONDARY.nameUi),
      )
   }
}