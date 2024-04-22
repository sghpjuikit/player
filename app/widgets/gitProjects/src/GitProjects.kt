package gitProjects

import de.jensd.fx.glyphs.GlyphIcons
import java.io.File
import java.io.File.separatorChar
import java.util.Stack
import javafx.geometry.Insets
import javafx.geometry.Orientation.VERTICAL
import javafx.geometry.Pos
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.TOP_RIGHT
import javafx.geometry.Side
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.input.KeyCode.BACK_SPACE
import javafx.scene.input.KeyCode.F5
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.BACK
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Priority.ALWAYS
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.Events.FileEvent
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.IconOC
import sp.it.pl.main.WidgetTags.DEVELOPMENT
import sp.it.pl.main.WidgetTags.UTILITY
import sp.it.pl.main.configure
import sp.it.pl.main.contextMenuFor
import sp.it.pl.main.emScaled
import sp.it.pl.main.listBox
import sp.it.pl.main.listBoxRow
import sp.it.pl.ui.objects.MdNode
import sp.it.pl.ui.objects.form.Validated
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.Util.filenamizeString
import sp.it.util.access.v
import sp.it.util.async.runVT
import sp.it.util.collections.setTo
import sp.it.util.collections.setToOne
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.Constraint.FileActor.DIRECTORY
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.conf.noUi
import sp.it.util.conf.only
import sp.it.util.file.FileType
import sp.it.util.file.children
import sp.it.util.file.div
import sp.it.util.file.hasExtension
import sp.it.util.file.toFast
import sp.it.util.file.toFileOrNull
import sp.it.util.functional.Try
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.ifNull
import sp.it.util.functional.net
import sp.it.util.functional.sortedByMemoized
import sp.it.util.functional.traverse
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.zip
import sp.it.util.reactive.zip2
import sp.it.util.system.open
import sp.it.util.text.capitalLower
import sp.it.util.text.equalsNc
import sp.it.util.text.nameUi
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.scrollPane
import sp.it.util.ui.separator
import sp.it.util.ui.show
import sp.it.util.ui.stackPane
import sp.it.util.ui.textArea
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.version
import sp.it.util.units.year

class GitProjects(widget: Widget): SimpleController(widget) {

   val inputFile by cvn<File>(null).only(DIRECTORY).def(name = "Projects root")
   val selection by cv("").noUi()
   val edit = v(false)
   val projects = mutableListOf<Project>()
   val contentPr = scrollPane()
   val contentEd = vBox(15.emScaled, Pos.CENTER)
   val contentEdArea = textArea()
   val contentMd = MdNode()
   val mdHistory = Stack<File>()

   init {
      root.prefSize = 400.emScaled x 400.emScaled
      root.onEventDown(KEY_PRESSED, F5) { refreshProjects() }
      root.onEventDown(KEY_PRESSED, BACK_SPACE) { visitPreviousFile() }
      root.onEventDown(MOUSE_CLICKED, SECONDARY) { visitPreviousFile() }
      root.onEventDown(MOUSE_CLICKED, BACK) { visitPreviousFile() }
      root.lay += hBox(20.emScaled, CENTER) {
         lay += stackPane {
            lay += stackPane {
               lay(TOP_RIGHT) += hBox(0.0, TOP_RIGHT) {
                  lay += Icon(IconFA.PLUS).apply {
                     disableProperty() syncFrom inputFile.isNull
                     tooltip("New project folder")
                     onClickDo { createProject() }
                  }
                  lay += Icon(IconFA.EDIT).apply {
                     disableProperty() syncFrom (selection zip edit zip2 inputFile).map { (s, e, l) -> s.isEmpty() || e || l==null }
                     tooltip("Edit project")
                     onClickDo { edit() }
                  }
               }
            }
            lay(CENTER, Insets(50.emScaled, 0.0, 50.emScaled, 0.0)) += stackPane {
               minWidth = 220.emScaled
               lay += contentPr.apply {
                  isFitToHeight = true
                  isFitToWidth = true
                  vbarPolicy = NEVER
                  hbarPolicy = NEVER
               }
            }
         }
         lay += separator(VERTICAL) { maxHeight = 200.emScaled }

         contentEd.apply {
            lay(ALWAYS) += contentEdArea
            lay += hBox(15.0, CENTER) {
               lay += Icon(IconFA.SAVE, 22.0).onClickDo { editDo(true) }.withText(Side.RIGHT, "Save")
               lay += Icon(IconMA.DO_NOT_DISTURB, 22.0).onClickDo { editDo(false) }.withText(Side.RIGHT, "Cancel")
            }
         }
         contentMd.apply {
            uriHandler.value = {
               it.toFileOrNull()?.takeIf { it hasExtension "md" }
                  .ifNotNull(::visitFile)
                  .ifNull(it::open)
            }
         }
         lay(ALWAYS) += stackPane {
            edit sync { lay.children.setToOne(if (it) contentEd else contentMd) }
         }
      }

      inputFile.sync { refreshProjects(it) } on onClose

      APP.actionStream.onEvent<FileEvent.Delete> { d -> if (projects.any { it.dir==d.file }) refreshProjects() } on onClose
   }

   fun refreshProjects(root: File? = inputFile.value) {
      runVT {
         root?.listProjects().orEmpty().map(::Project).toList()
      } ui {
         projects setTo it
         contentPr.content = listBox { lay += projects.map { it.row } }
         projects.find { it.name==selection.value }?.select(true)
      }
   }

   fun File.listProjects(depth: Int = 3): Sequence<File> =
      sequence {
         if (depth>=2)
            yield(this@listProjects.toFast())
         if (depth>0) {
            val children = children()
            children.forEach {
               when (FileType(it)) {
                  FileType.FILE ->
                     if (it hasExtension "md" && !it.name.equalsNc("README.md")) yield(it.toFast(FileType.FILE))
                  FileType.DIRECTORY ->
                     if (children.any { it.name.equalsNc("README.md") }) yield(this@listProjects.toFast(FileType.DIRECTORY))
                     else if (children.any { it.name == ".git" }) yield(this@listProjects.toFast(FileType.DIRECTORY))
                     else yieldAll(it.listProjects(depth-1))
               }
            }
         }
      }
      // always include project parent dirs
      .flatMap { it.traverse { it.parentFile }.takeWhile { it != inputFile.value } }
      .distinct()
      .sortedByMemoized { it.path.lowercase() }

   fun visitProject(p: Project?) {
      mdHistory.clear()
      visitFile(p?.mdFile)
   }

   fun visitFile(f: File?) {
      if (f!=null) mdHistory.push(f)
      contentMd.readFile(f)
   }

   fun visitPreviousFile() {
      if (mdHistory.size<=1) return
      mdHistory.pop()
      contentMd.readFile(mdHistory.peek())
   }

   fun edit() {
      if (edit.value) return
      contentEdArea.text = contentMd.text.value
      edit.value = true
   }

   fun editDo(submit: Boolean) {
      if (!edit.value) return
      edit.value = false
      if (submit) mdHistory.peek()!!.writeText(contentEdArea.text)
      if (submit) contentMd.readFile(mdHistory.peek())
      contentEdArea.text = null
   }

   fun createProject() {
      val dir = inputFile.value ?: return
      object: ConfigurableBase<Any?>(), Validated {
         val name by cvn<String>(null)
         val type by cv(FileType.FILE)
         override fun isValid(): Try<Nothing?, String> =
            if (name.value==null) Try.Error("Name must not be null")
            else if (filenamizeString(name.value)!=name.value) Try.Error("Name must be valid filename")
            else Try.Ok(null)
      }.configure("New project") {
         when (it.type.value) {
            FileType.FILE -> (dir / "${it.name.value!!}.md").createNewFile()
            FileType.DIRECTORY -> (dir / it.name.value!!).mkdirs()
         }
         refreshProjects()
      }
   }

   inner class Project(val dir: File) {
      val name = dir.name.capitalLower()
      val depth = inputFile.value?.net { dir.relativeTo(it).path.count { it==separatorChar } } ?: 0
      val children = if (dir.isDirectory) dir.children().toList() else listOf()
      val gitDir = children.find { it.name == ".git" }
      val readmeFile = children.find { it.name.equalsNc("README.md") }
      val mdFile = readmeFile ?: dir.takeIf { it hasExtension "md" }
      val glyph: GlyphIcons = when {
         dir.isFile && dir hasExtension "md" -> IconOC.MARKDOWN
         gitDir!=null -> IconFA.GIT
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
         if (s) projects.find { it!==this && it.name==selection.value }?.select(false)
         if (s) selection.value = name
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