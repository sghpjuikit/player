package sp.it.pl.main

import org.atteo.evo.inflector.English
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.PlaylistSong
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.gui.pane.ActionPane
import sp.it.pl.gui.pane.OverlayPane
import sp.it.pl.gui.pane.ShortcutPane
import sp.it.pl.layout.Component
import sp.it.pl.layout.container.Container
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.io.InOutput
import sp.it.pl.layout.widget.controller.io.Input
import sp.it.pl.layout.widget.controller.io.Output
import sp.it.pl.layout.widget.feature.Feature
import sp.it.pl.plugin.Plugin
import sp.it.util.access.fieldvalue.FileField
import sp.it.util.dev.fail
import sp.it.util.file.FileType
import sp.it.util.file.Util
import sp.it.util.functional.getOr
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.on
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncBiFrom
import sp.it.util.reactive.syncTo
import sp.it.util.type.ClassName
import sp.it.util.type.InstanceInfo
import sp.it.util.type.InstanceName
import sp.it.util.type.ObjectFieldMap
import sp.it.util.type.Util.getRawGenericPropertyType
import sp.it.util.ui.image.getImageDim
import sp.it.util.units.FileSize
import java.io.File
import java.util.function.Consumer
import kotlin.reflect.KClass

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
   infix fun KClass<*>.touch(unit: Unit) = unit
   PlaylistSong::class touch Unit
   Metadata::class touch Unit
   MetadataGroup::class touch Unit
   Any::class touch Unit
   File::class touch Unit
}

fun ClassName.initApp() {
   addNoLookup(Void::class.java, "Nothing")
   add(String::class.java, "Text")
   add(File::class.java, "File")
   add(App::class.java, "Application")
   add(Song::class.java, "Song")
   add(PlaylistSong::class.java, "Playlist Song")
   add(Metadata::class.java, "Library Song")
   add(MetadataGroup::class.java, "Song Group")
   add(Plugin::class.java, "Plugin")
   add(Widget::class.java, "Widget")
   add(Container::class.java, "Container")
   add(Input::class.java, "Input")
   add(Output::class.java, "Output")
   add(InOutput::class.java, "In-Output")
   add(Feature::class.java, "Feature")
   add(List::class.java, "List")
}

fun InstanceName.initApp() {
   add(Void::class.java) { "<none>" }
   add(File::class.java) { it.path }
   add(App::class.java) { "This application" }
   add(Song::class.java) { it.getPathAsString() }
   add(PlaylistSong::class.java) { it.getTitle() }
   add(Metadata::class.java) { it.getTitleOrEmpty() }
   add(MetadataGroup::class.java) { it.getValueS("<none>") }
   add(Plugin::class.java) { it.name }
   add(Component::class.java) { it.exportName }
   add(Feature::class.java) { "Feature" }
   add(Input::class.java) { it.name }
   add(Output::class.java) { it.name }
   add(InOutput::class.java) { it.o.name }
   add(Collection::class.java) {
      val eType = getRawGenericPropertyType(it.javaClass)
      val eName = if (eType==it.javaClass || eType==null || eType==Any::class.java) "Item" else APP.className[eType]
      it.size.toString() + " " + English.plural(eName, it.size)
   }
}

fun InstanceInfo.initApp() {
   add(Void::class.java) { _, _ -> }
   add(String::class.java) { s, map -> map["Length"] = (s?.length ?: 0).toString() }
   add(File::class.java) { f, map ->
      val type = FileType(f)
      map["File type"] = type.name

      if (type==FileType.FILE) {
         val fs = FileSize(f)
         map["Size"] = "" + fs + (if (fs.isKnown()) " (%,d bytes)".format(fs.inBytes()).replace(',', ' ') else "")
         map["Format"] = f.name.substringAfterLast('.', "<none>")
      }

      map[FileField.TIME_CREATED.name()] = FileField.TIME_CREATED.getOfS(f, "n/a")
      map[FileField.TIME_MODIFIED.name()] = FileField.TIME_MODIFIED.getOfS(f, "n/a")

      if (f.isImage()) {
         val res = getImageDim(f).map { "${it.width} x ${it.height}" }.getOr("n/a")
         map["Resolution"] = res
      }
   }
   add(App::class.java) { v, map -> map["Name"] = v.name }
   add(Component::class.java) { v, map -> map["Name"] = v.exportName }
   add(Metadata::class.java) { m, map ->
      Metadata.Field.all.asSequence()
         .filter { it.isTypeStringRepresentable() && !it.isFieldEmpty(m) }
         .forEach { map[it.name()] = it.getOfS(m, "<none>") }
   }
   add(PlaylistSong::class.java) { p, map ->
      PlaylistSong.Field.all.asSequence()
         .filter { it.isTypeStringRepresentable() }
         .forEach { map[it.name()] = it.getOfS(p, "<none>") }
   }
   add(Feature::class.java) { f, map ->
      map["Name"] = f.name
      map["Description"] = f.description
   }
}

fun <T, P: OverlayPane<T>> P.initApp() = apply {
   val d = Disposer()
   APP.ui.viewDisplay syncTo display on d
   display sync { if (it is OverlayPane.Display) APP.ui.viewDisplay.value = it } on d
   displayBgr syncBiFrom APP.ui.viewDisplayBgr on d
   onHidden += d
}

fun ActionPane.initApp() = apply {
   val d = Disposer()
   (this as OverlayPane<*>).initApp()
   closeOnDone syncBiFrom APP.ui.viewCloseOnDone on d
   onHidden += d
}

fun ShortcutPane.initApp() = apply {
   val d = Disposer()
   (this as OverlayPane<*>).initApp()
   hideEmptyShortcuts syncBiFrom APP.ui.viewHideEmptyShortcuts on d
   onHidden += d
   onHidden += {
      APP.actionStream("Shortcuts")
   }
}