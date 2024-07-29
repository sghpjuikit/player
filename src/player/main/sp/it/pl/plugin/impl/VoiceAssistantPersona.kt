package sp.it.pl.plugin.impl


import java.io.File
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
import sp.it.pl.layout.WidgetFactory
import sp.it.pl.layout.controller.ControllerIntro
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.Bool
import sp.it.pl.main.Events.FileEvent
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.WidgetTags.DEVELOPMENT
import sp.it.pl.main.WidgetTags.UTILITY
import sp.it.pl.main.configure
import sp.it.pl.main.emScaled
import sp.it.pl.main.listBox
import sp.it.pl.main.listBoxRow
import sp.it.pl.main.withAppProgress
import sp.it.pl.plugin.impl.VoiceAssistant
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
import sp.it.util.conf.Constraint
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.filename
import sp.it.util.conf.noUi
import sp.it.util.file.FileType
import sp.it.util.file.children
import sp.it.util.file.div
import sp.it.util.file.hasExtension
import sp.it.util.file.readTextTry
import sp.it.util.file.toFileOrNull
import sp.it.util.file.writeTextTry
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Error
import sp.it.util.functional.Try.Ok
import sp.it.util.functional.and
import sp.it.util.functional.getOr
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.ifNull
import sp.it.util.reactive.map
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.zip
import sp.it.util.reactive.zip2
import sp.it.util.system.open
import sp.it.util.text.capitalLower
import sp.it.util.text.nameUi
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.scrollPane
import sp.it.util.ui.separator
import sp.it.util.ui.stackPane
import sp.it.util.ui.textArea
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.version
import sp.it.util.units.year

class VoiceAssistantPersona(widget: Widget): SimpleController(widget) {

   private val plugin = APP.plugins.plugin<VoiceAssistant>().asValue(onClose)
   val selection by cv("").noUi()
   val edit = v(false)
   val personas = mutableListOf<Persona>()
   val contentPr = scrollPane()
   val contentEd = vBox(15.emScaled, Pos.CENTER)
   val contentEdArea = textArea()
   val contentTx = textArea()

   init {
      root.prefSize = 650.emScaled x 400.emScaled

      root.disableProperty() syncFrom (plugin map { it==null })
      root.onEventDown(KEY_PRESSED, F5) { refreshPersonas() }
      root.lay += hBox(20.emScaled, CENTER) {
         lay += stackPane {
            lay += stackPane {
               lay(TOP_RIGHT) += hBox(0.0, TOP_RIGHT) {
                  lay += Icon(IconMA.PERSON_ADD).apply {
                     tooltip("New Persona")
                     onClickDo { createPersona() }
                  }
                  lay += Icon(IconFA.EDIT).apply {
                     disableProperty() syncFrom (selection zip edit).map { (s, e) -> s.isEmpty() || e }
                     tooltip("Edit persona")
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
            lay(ALWAYS) += contentEdArea.apply {
               isWrapText = true
            }
            lay += hBox(15.0, CENTER) {
               lay += Icon(IconFA.SAVE, 22.0).onClickDo { editDo(true) }.withText(Side.RIGHT, "Save")
               lay += Icon(IconMA.DO_NOT_DISTURB, 22.0).onClickDo { editDo(false) }.withText(Side.RIGHT, "Cancel")
            }
         }
         contentTx.apply {
            isWrapText = true
            isEditable = false
         }
         lay(ALWAYS) += stackPane {
            edit sync { lay.children.setToOne(if (it) contentEd else contentTx) }
         }
      }

      root.sync1IfInScene { refreshPersonas() } on onClose
      root.sync1IfInScene { selection.value = selectedPersona()?.name ?: personas.firstOrNull()?.name ?: "" } on onClose

      APP.actionStream.onEvent<FileEvent.Delete> { e -> if (personas.any { it.def==e.file }) refreshPersonas() } on onClose
   }

   fun refreshPersonas() {
      runVT {
         listPersonas().orEmpty().toList()
      } ui {
         personas setTo it
         contentPr.content = listBox { lay += personas.map { it.row } }
         personas.find { it.name==selection.value }?.select(true)
      }
   }

   fun selectedPersona() =
      personas.find { it.name==selection.value }

   fun listPersonas() =
      VoiceAssistant.obtainPersonas().map(::Persona)

   fun visitPersona(p: Persona) {
      contentTx.text = p.def.readTextTry().getOr("Failed to read persona file")
   }

   fun edit() {
      if (edit.value) return
      contentEdArea.text = contentTx.text
      edit.value = true
   }

   fun editDo(submit: Boolean) {
      if (!edit.value) return
      edit.value = false
      if (submit) selectedPersona()?.editDef(contentEdArea.text.orEmpty())
      contentEdArea.text = null
   }

   fun createPersona() {
      object: ConfigurableBase<Any?>(), Validated {
         val name by cvn<String>(null).filename()
         override fun isValid() =
            if (name.value==null) Error(Constraint.ObjectNonNull.message())
            else if (listPersonas().map { it.name }.contains(name.value!!)) Error("Persona already exists")
            else Ok(null)
      }.configure("New persona") {
         (VoiceAssistant.dirPersonas / "${it.name.value!!}.txt").createNewFile()
         refreshPersonas()
      }
   }

   inner class Persona(val def: File) {
      val name = def.nameWithoutExtension.capitalLower()
      val row = listBoxRow(icon(), name) {
         icon.onClickDo(PRIMARY, null) { _, e ->
            if (e==null) this@Persona.select(true)
            if (e?.clickCount==1) this@Persona.select(true)
            if (e?.clickCount==2) this@Persona.activate()
         }
      }

      fun icon() = if (isActive()) IconMA.PERSON else IconMA.PERSON_OUTLINE

      fun isActive() = plugin.value?.llmChatSysPromptFile?.value == def

      fun activate() {
         if (isActive()) return
         plugin.value?.llmChatSysPromptFile?.value = def
         personas.forEach { it.row.icon.icon(it.icon()) }
      }

      fun select(s: Boolean) {
         row.select(s)
         if (s) personas.find { it!==this && it.name==selection.value }?.select(false)
         if (s) selection.value = name
         if (s) visitPersona(this)
      }

      fun editDef(text: String) =
         runVT { def.writeTextTry(text) }.ui {
            if (isActive()) plugin.value?.llmChatSysPrompt?.value = text
            this@VoiceAssistantPersona.contentTx.text = text
         }.withAppProgress(
            "Edit persona $name"
         )
   }

   companion object: WidgetCompanion {
      override val name = "Voice Assistant persona editor"
      override val description = "Manage voice assistant personas"
      override val descriptionLong = "$description."
      override val icon = IconMA.PERSON
      override val version = version(1, 1, 0)
      override val isSupported = true
      override val year = year(2024)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(UTILITY)
      override val summaryActions = listOf(
         Entry("Data", "Refresh personas", F5.nameUi)
      )
   }
}