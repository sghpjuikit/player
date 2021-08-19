package sp.it.pl.layout

import javafx.geometry.Orientation
import sp.it.pl.main.APP
import sp.it.pl.main.AppError
import sp.it.pl.main.ifErrorNotify
import sp.it.util.async.runIO
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.properties.PropVal
import sp.it.util.file.writeTextTry
import sp.it.util.functional.orNull
import sp.it.util.text.splitTrimmed
import java.io.File
import java.util.UUID

private val json = APP.serializerJson.json

/**
 * Creates a launcher for this component as given file. Launcher is a
 * file, opening which by this application opens this component with its
 * current settings.
 *
 * See [File.loadComponentFxwlJson]
 */
fun Component.exportFxwl(f: File) = runIO {
   APP.serializerJson.toJson(toDb(), f).ifErrorNotify {
      AppError(
         "Unable to export component launcher for $name into $f.",
         "Reason:\n${it.stacktraceAsString}"
      )
   }
}

/** Creates a launcher for this widget with default (no predefined) settings.  */
fun Widget.exportFxwlDefault(f: File) = runIO {
   f.writeTextTry(factory.id).ifErrorNotify {
      AppError(
         "Unable to export default widget launcher for $name into $f.",
         "Reason:\n${it.stacktraceAsString}"
      )
   }
}

/**
 * Loads component launcher.
 * See [Component.exportFxwl]
 */
fun File.loadComponentFxwlJson() = runIO {
   json.fromJson<ComponentDb>(this).ifErrorNotify {
      AppError(
         "Unable to load component launcher from $this.",
         "Reason:\n${it.stacktraceAsString}"
      )
   }.orNull()?.deduplicateIds()?.toDomain()
}

/** Regenerates component ids to avoid duplicates when loading same component multiple times. This is always necessary. */
fun ComponentDb.deduplicateIds(): ComponentDb {
   val ids = mutableMapOf<UUID, UUID>()
   fun UUID.dd() = ids.getOrPut(this) { UUID.randomUUID() }
   fun UUID.ddOrSame() = ids[this] ?: this
   fun Map<String, Any?>.dd() = mapValues { (k, v) ->
      if (k.startsWith("io") && v is String) {
         v.splitTrimmed(":").joinToString(":") {
            v.substringBeforeLast(",") + "," + UUID.fromString(v.substringAfterLast(",")).ddOrSame().toString()
         }
      } else {
         v
      }
   }

   fun ComponentDb.dd(): ComponentDb = when (this) {
      is NoComponentDb -> this
      is RootContainerDb -> RootContainerDb(id.dd(), loading, locked, child?.dd(), properties)
      is SwitchContainerDb -> SwitchContainerDb(id.dd(), translate, loading, locked, children.mapValues { it.value?.dd() }, properties)
      is UniContainerDb -> UniContainerDb(id.dd(), loading, locked, child?.dd(), properties)
      is BiContainerDb -> BiContainerDb(id.dd(), orientation, position, absoluteSize, collapsed, joined, loading, locked, children.mapValues { it.value?.dd() }, properties)
      is FreeFormContainerDb -> FreeFormContainerDb(id.dd(), loading, locked, showHeaders, children.mapValues { it.value?.dd() }, properties)
      is WidgetDb -> WidgetDb(id.dd(), factoryId, nameUi, loading, locked, properties, settings)
   }

   fun ComponentDb.dd2(): ComponentDb = when (this) {
      is NoComponentDb -> this
      is RootContainerDb -> RootContainerDb(id, loading, locked, child?.dd2(), properties)
      is SwitchContainerDb -> SwitchContainerDb(id, translate, loading, locked, children.mapValues { it.value?.dd2() }, properties)
      is UniContainerDb -> UniContainerDb(id, loading, locked, child?.dd2(), properties)
      is BiContainerDb -> BiContainerDb(id, orientation, position, absoluteSize, collapsed, joined, loading, locked, children.mapValues { it.value?.dd2() }, properties)
      is FreeFormContainerDb -> FreeFormContainerDb(id, loading, locked, showHeaders, children.mapValues { it.value?.dd2() }, properties)
      is WidgetDb -> WidgetDb(id, factoryId, nameUi, loading, locked, properties.dd(), settings)
   }

   return this.dd().dd2()
}

sealed class ComponentDb(
   val id: UUID = UUID.randomUUID(),
   val loading: Widget.LoadType = Widget.LoadType.AUTOMATIC,
   val locked: Boolean = false,
   val properties: Map<String, Any?> = mapOf()
) {
   abstract fun toDomain(): Component?
}

object NoComponentDb: ComponentDb() {
   override fun toDomain() = null
}

class RootContainerDb(
   id: UUID = UUID.randomUUID(),
   loading: Widget.LoadType = Widget.LoadType.AUTOMATIC,
   locked: Boolean = false,
   val child: ComponentDb? = null,
   properties: Map<String, Any?> = mapOf()
): ComponentDb(id, loading, locked, properties) {
   fun toUni() = UniContainerDb(id, loading, locked, child, properties)
   override fun toDomain() = Layout(this)
}

class SwitchContainerDb(
   id: UUID = UUID.randomUUID(),
   val translate: Double = 0.0,
   loading: Widget.LoadType = Widget.LoadType.AUTOMATIC,
   locked: Boolean = false,
   val children: Map<Int, ComponentDb?> = mapOf(),
   properties: Map<String, Any?> = mapOf()
): ComponentDb(id, loading, locked, properties) {
   override fun toDomain() = ContainerSwitch(this)
}

class UniContainerDb(
   id: UUID = UUID.randomUUID(),
   loading: Widget.LoadType = Widget.LoadType.AUTOMATIC,
   locked: Boolean = false,
   val child: ComponentDb? = null,
   properties: Map<String, Any?> = mapOf()
): ComponentDb(id, loading, locked, properties) {
   override fun toDomain() = ContainerUni(this)
}

class BiContainerDb(
   id: UUID = UUID.randomUUID(),
   val orientation: Orientation = Orientation.VERTICAL,
   val position: Double = 0.5,
   val absoluteSize: Int = 0,
   val collapsed: Int = 0,
   val joined: Boolean = false,
   loading: Widget.LoadType = Widget.LoadType.AUTOMATIC,
   locked: Boolean = false,
   val children: Map<Int, ComponentDb?> = mapOf(),
   properties: Map<String, Any?> = mapOf()
): ComponentDb(id, loading, locked, properties) {
   override fun toDomain() = ContainerBi(this)
}

class FreeFormContainerDb(
   id: UUID = UUID.randomUUID(),
   loading: Widget.LoadType = Widget.LoadType.AUTOMATIC,
   locked: Boolean = false,
   val showHeaders: Boolean = true,
   val children: Map<Int, ComponentDb?> = mapOf(),
   properties: Map<String, Any?> = mapOf()
): ComponentDb(id, loading, locked, properties) {
   override fun toDomain() = ContainerFreeForm(this)
}

class WidgetDb(
   id: UUID = UUID.randomUUID(),
   val factoryId: String = emptyWidgetFactory.id,
   val nameUi: String = emptyWidgetFactory.name,
   loading: Widget.LoadType = Widget.LoadType.AUTOMATIC,
   locked: Boolean = false,
   properties: Map<String, Any?> = mapOf(),
   val settings: Map<String, PropVal?> = mapOf()
): ComponentDb(id, loading, locked, properties) {
   override fun toDomain() = Widget(this)
}