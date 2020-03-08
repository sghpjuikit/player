package sp.it.pl.main

import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.PlaylistSong
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.ui.pane.ActionPane
import sp.it.pl.ui.pane.OverlayPane
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.pl.layout.Component
import sp.it.pl.layout.container.Container
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.io.InOutput
import sp.it.pl.layout.widget.controller.io.Input
import sp.it.pl.layout.widget.controller.io.Output
import sp.it.pl.layout.widget.feature.Feature
import sp.it.pl.plugin.PluginBase
import sp.it.util.access.fieldvalue.ColumnField
import sp.it.util.access.fieldvalue.FileField
import sp.it.util.access.fieldvalue.IconField
import sp.it.util.dev.fail
import sp.it.util.file.FileType
import sp.it.util.file.Util
import sp.it.util.functional.getOr
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.on
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncBiFrom
import sp.it.util.reactive.syncTo
import sp.it.util.text.pluralUnit
import sp.it.util.type.ClassName
import sp.it.util.type.InstanceDescription
import sp.it.util.type.InstanceName
import sp.it.util.type.ObjectFieldMap
import sp.it.util.ui.image.getImageDim
import sp.it.util.units.FileSize
import java.io.File
import java.util.function.Consumer

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
            "List" alias List::class
   // @formatter:on
}

fun InstanceName.initApp() {
   add(Any::class.java) { "object" }
   add(Void::class.java) { "<none>" }
   add(File::class.java) { it.path }
   add(App::class.java) { "This application" }
   add(Song::class.java) { it.getPathAsString() }
   add(PlaylistSong::class.java) { it.getTitle() }
   add(Metadata::class.java) { it.getTitleOrEmpty() }
   add(MetadataGroup::class.java) { it.getValueS("<none>") }
   add(PluginBase::class.java) { it.name }
   add(Component::class.java) { it.name }
   add(Feature::class.java) { "Feature" }
   add(Input::class.java) { it.name }
   add(Output::class.java) { it.name }
   add(InOutput::class.java) { it.o.name }
   add(Collection::class.java) {
      val eType = it::class.supertypes.find { it.classifier==Collection::class }?.arguments?.getOrNull(0)?.type
      val eName = eType?.toUi() ?: "Item"
      eName.pluralUnit(it.size)
   }
}

fun InstanceDescription.initApp() {
   String::class describe {
      "Length" info it.length.toString()
      "Lines" info it.lineSequence().count().toString()
   }
   File::class describe { f ->
      val type = FileType(f)
      "File type" info type.name

      if (type==FileType.FILE) {
         val fs = FileSize(f)
         "Size" info ("" + fs + (if (fs.isKnown()) " (%,d bytes)".format(fs.inBytes()).replace(',', ' ') else ""))
         "Format" info f.name.substringAfterLast('.', "<none>")
         FileField.MIME.name() info FileField.MIME.getOfS(f, "n/a")
      }

      FileField.TIME_CREATED.name() info FileField.TIME_CREATED.getOfS(f, "n/a")
      FileField.TIME_MODIFIED.name() info FileField.TIME_MODIFIED.getOfS(f, "n/a")
      FileField.IS_HIDDEN.name() info FileField.IS_HIDDEN.getOfS(f, "n/a")

      if (f.isImage())
         "Resolution" info getImageDim(f).map { "${it.width} x ${it.height}" }.getOr("n/a")
   }
   App::class describe {
      "Name" info it.name
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
   // onHidden += d  // TODO: this can only work if OverlayPane is one-use, which it isnt
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