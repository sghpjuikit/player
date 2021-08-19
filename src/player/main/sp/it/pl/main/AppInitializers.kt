package sp.it.pl.main

import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.PlaylistSong
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.layout.Component
import sp.it.pl.layout.Container
import sp.it.pl.layout.Widget
import sp.it.pl.layout.controller.io.InOutput
import sp.it.pl.layout.controller.io.Input
import sp.it.pl.layout.controller.io.Output
import sp.it.pl.layout.feature.Feature
import sp.it.pl.plugin.PluginBase
import sp.it.pl.ui.pane.ActionPane
import sp.it.pl.ui.pane.OverlayPane
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.access.fieldvalue.ColumnField
import sp.it.util.access.fieldvalue.FileField
import sp.it.util.access.fieldvalue.IconField
import sp.it.util.collections.getElementType
import sp.it.util.dev.fail
import sp.it.util.file.FileType
import sp.it.util.file.Util
import sp.it.util.functional.getOr
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.on
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncBiFrom
import sp.it.util.reactive.syncTo
import sp.it.util.text.Char16
import sp.it.util.text.Char32
import sp.it.util.text.lengthInChars
import sp.it.util.text.lengthInCodePoints
import sp.it.util.text.lengthInGraphemes
import sp.it.util.text.pluralUnit
import sp.it.util.text.toChar32
import sp.it.util.type.ClassName
import sp.it.util.type.InstanceDescription
import sp.it.util.type.InstanceName
import sp.it.util.type.ObjectFieldMap
import sp.it.util.ui.image.getImageDim
import sp.it.util.units.FileSize
import java.io.File
import java.nio.file.attribute.FileTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Date
import java.util.function.Consumer
import javafx.util.Duration
import kotlin.streams.asSequence

fun File.verify() {
   if (!isAbsolute)
      fail { "File $this is not absolute" }

   if (!Util.isValidatedDirectory(this))
      fail { "File $this is not accessible" }
}

fun AppInstanceComm.initApp() {
   onNewInstanceHandlers += Consumer { APP.parameterProcessor.process(it) }
}

fun ObjectFieldMap.initApp() {
   infix fun Any.touch(unit: Unit) = unit
   // @formatter:off
    PlaylistSong.Field touch Unit
        Metadata.Field touch Unit
   MetadataGroup.Field touch Unit
           ColumnField touch Unit
             FileField touch Unit
             IconField touch Unit
   // @formatter:on
}

fun ClassName.initApp() {
   // @formatter:off
         "Nothing" aliasExact Nothing::class
         "Nothing" aliasExact Void::class
          "Object" aliasExact Any::class
       "Character" alias Char16::class
       "Character" alias Char32::class
            "Text" alias String::class
            "File" alias File::class
     "Application" alias App::class
            "Song" alias Song::class
   "Playlist Song" alias PlaylistSong::class
    "Library Song" alias Metadata::class
      "Song Group" alias MetadataGroup::class
          "Plugin" alias PluginBase::class
          "Widget" alias Widget::class
       "Container" alias Container::class
           "Input" alias Input::class
          "Output" alias Output::class
       "In-Output" alias InOutput::class
         "Feature" alias Feature::class
      "Collection" alias Collection::class
            "List" alias List::class
             "Set" alias Set::class
             "Map" alias Map::class
            "Time" alias FileTime::class
            "Time" alias LocalTime::class
            "Date" alias LocalDate::class
        "DateTime" alias LocalDateTime::class
        "DateTime" alias Date::class
        "Duration" alias java.time.Duration::class
        "Duration" alias Duration::class
   // @formatter:on
}

fun InstanceName.initApp() {
   add(Void::class) { "<none>" }
   add(Nothing::class) { "<none>" }
   add(Any::class) {
      if (it::class==Any::class) "object"
      else it.toUi()
   }
   add(String::class) {
      if (it.lengthInCodePoints>40) it.codePoints().asSequence().take(41).joinToString("") { it.toChar32().toString() } + " (first 40 characters)"
      else it
   }
   add(App::class) { "This application" }
   add(Song::class) { it.getPathAsString() }
   add(PlaylistSong::class) { it.getTitle() }
   add(Metadata::class) { it.getTitleOrEmpty() }
   add(MetadataGroup::class) { it.getValueS("<none>") }
   add(PluginBase::class) { it.name }
   add(Component::class) { it.name }
   add(Feature::class) { "Feature" }
   add(Input::class) { it.name }
   add(Output::class) { it.name }
   add(InOutput::class) { it.o.name }
   add(Collection::class) {
      val eName = null
         ?: it::class.supertypes.find { it.classifier==Collection::class }?.let { it.arguments[0].type.toUi() }
         ?: if (it.isEmpty()) "Empty ${it::class.toUi()}" else null
         ?: it.getElementType().toUi()
      eName.pluralUnit(it.size)
   }
   add(Map::class) {
      val eName = null
         ?: it::class.supertypes.find { it.classifier==Collection::class }?.arguments?.let { "Pair<" + it[0].type.toUi() + " -> " + it[1].type.toUi() + ">" }
         ?: if (it.isEmpty()) "Empty ${it::class.toUi()}" else null
         ?: "Pair<" + it.keys.getElementType().toUi() + " -> " + it.values.getElementType().toUi() + ">"
      eName.pluralUnit(it.size)
   }
}

fun InstanceDescription.initApp() {
   Char::class describe {
      "Bin" info "0b" + Integer.toBinaryString(it.code)
      "Oct" info "0" + Integer.toOctalString(it.code)
      "Dec" info it.code.toUi()
      "Hex" info "0x" + Integer.toHexString(it.code)
      "Unicode" info "U+" + Integer.toHexString(it.code).padStart(4, '0')

      "Escape sequence" info "\\u" + Integer.toHexString(it.code).padStart(4, '0')
   }
   Char32::class describe {
      "Bin" info "0b" + Integer.toBinaryString(it.toInt())
      "Oct" info "0" + Integer.toOctalString(it.toInt())
      "Dec" info it.toInt().toUi()
      "Hex" info "0x" + Integer.toHexString(it.toInt())
      "Unicode" info "U+" + Integer.toHexString(it.toInt()).padStart(4, '0')
   }
   String::class describe {
      "Length (char)" info it.lengthInChars.toUi()
      "Length (code point)" info it.lengthInCodePoints.toUi()
      "Length (grapheme)" info it.lengthInGraphemes.toUi()
      "Lines" info it.lineSequence().count().toUi()
   }
   File::class describe { f ->
      val type = FileType(f)
      val exists = f.exists()

      "File type" info type.toUi()
      "Exists" info exists.toUi()

      if (type==FileType.FILE) {
         val fs = FileSize(f)

         if (exists)
            "Size" info ("" + fs + (if (fs.isKnown()) " (%,d bytes)".format(fs.inBytes()).replace(',', ' ') else "n/a"))

         "Format" info f.name.substringAfterLast('.', "<none>")
         FileField.MIME.name() info FileField.MIME.getOfS(f, "n/a")
      }

      if (exists) {
         FileField.TIME_CREATED.name() info FileField.TIME_CREATED.getOfS(f, "n/a")
         FileField.TIME_MODIFIED.name() info FileField.TIME_MODIFIED.getOfS(f, "n/a")
         FileField.IS_HIDDEN.name() info FileField.IS_HIDDEN.getOfS(f, "n/a")

         // TODO: load all image/audio metadata
         if (f.isImage())
            "Resolution" info getImageDim(f).map { "${it.width} x ${it.height}" }.getOr("n/a")
      }

   }
   App::class describe {
      "Name" info it.name
   }
   AppError::class describe {
      "Text" info (it.textShort + "\n\n" + it.textFull)
   }
   Component::class describe {
      "Name" info it.name
   }
   Metadata::class describe { m ->
      Metadata.Field.all.asSequence()
         .filter { it.isTypeStringRepresentable() && !it.isFieldEmpty(m) }
         .forEach { it.name() info it.getOfS(m, "<none>") }
   }
   PlaylistSong::class describe { p ->
      PlaylistSong.Field.all.asSequence()
         .filter { it.isTypeStringRepresentable() }
         .forEach { it.name() info it.getOfS(p, "<none>") }
   }
   Feature::class describe {
      "Name" info it.name
      "Description" info it.description
   }
}

fun <T, P: OverlayPane<T>> P.initApp() = apply {
   val d = Disposer()
   APP.ui.viewDisplay syncTo display on d
   display sync { if (it is OverlayPane.Display) APP.ui.viewDisplay.value = it } on d
   displayBgr syncBiFrom APP.ui.viewDisplayBgr on d
   // onHidden += d  // TODO: this can only work if OverlayPane is one-use, which it isn't
}

fun ActionPane.initApp() = apply {
   val d = Disposer()
   (this as OverlayPane<*>).initApp()
   closeOnDone syncBiFrom APP.ui.viewCloseOnDone on d
   // onHidden += d
}

fun ShortcutPane.initApp() = apply {
   val d = Disposer()
   (this as OverlayPane<*>).initApp()
   hideEmptyShortcuts syncBiFrom APP.ui.viewHideEmptyShortcuts on d
   // onHidden += d
   onHidden += {
      APP.actionStream("Shortcuts")
   }
}