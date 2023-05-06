package sp.it.pl.layout

import java.io.File
import java.util.UUID
import javafx.geometry.Orientation
import javafx.geometry.Pos
import sp.it.pl.main.APP
import sp.it.pl.main.AppError
import sp.it.pl.main.ifErrorNotify
import sp.it.util.async.runVT
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.json.JsValue
import sp.it.util.file.writeTextTry
import sp.it.util.functional.orNull
import sp.it.util.text.splitNoEmpty

private val json = APP.serializerJson.json

/**[Component.toDb] or [NoComponentDb] */
fun Component?.toDb(): ComponentDb = this?.toDb() ?: NoComponentDb

/**
 * Creates a launcher for this component as given file. Launcher is a
 * file, opening which by this application opens this component with its
 * current settings.
 *
 * See [File.loadComponentFxwlJson]
 */
fun Component?.exportFxwl(f: File) = runVT {
   APP.serializerJson.toJson(toDb(), f).ifErrorNotify {
      AppError(
         "Unable to export component launcher for ${this?.name} into ${f}.",
         "Reason:\n${it.stacktraceAsString}"
      )
   }.orThrow
}

/** Creates a launcher for this widget with default (none predefined) settings.  */
fun Widget.exportFxwlDefault(f: File) = runVT {
   f.writeTextTry(factory.id).ifErrorNotify {
      AppError(
         "Unable to export default widget launcher for $name into $f.",
         "Reason:\n${it.stacktraceAsString}"
      )
   }.orThrow
}

/**
 * Loads component launcher.
 * See [Component.exportFxwl]
 */
fun File.loadComponentFxwlJson() = runVT {
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
         v.splitNoEmpty(":").joinToString(":") {
            v.substringBeforeLast(",") + "," + UUID.fromString(v.substringAfterLast(",")).ddOrSame().toString()
         }
      } else {
         v
      }
   }

   fun ComponentDb.dd(): ComponentDb = when (this) {
      is NoComponentDb -> this
      is ContainerRootDb -> ContainerRootDb(id.dd(), loading, locked, child?.dd(), properties)
      is SwitchContainerDb -> SwitchContainerDb(id.dd(), translate, loading, locked, children.mapValues { it.value?.dd() }, properties)
      is ContainerUniDb -> ContainerUniDb(id.dd(), loading, locked, child?.dd(), properties)
      is ContainerBiDb -> ContainerBiDb(id.dd(), orientation, position, absoluteSize, collapsed, joined, loading, locked, children.mapValues { it.value?.dd() }, properties)
      is ContainerSeqDb -> ContainerSeqDb(id.dd(), orientation, fill, alignment, joined, loading, locked, children.mapValues { it.value?.dd() }, properties)
      is ContainerFreeFormDb -> ContainerFreeFormDb(id.dd(), loading, locked, showHeaders, children.mapValues { it.value?.dd() }, properties)
      is WidgetDb -> WidgetDb(id.dd(), factoryId, nameUi, loading, locked, properties, fields)
   }

   fun ComponentDb.dd2(): ComponentDb = when (this) {
      is NoComponentDb -> this
      is ContainerRootDb -> ContainerRootDb(id, loading, locked, child?.dd2(), properties)
      is SwitchContainerDb -> SwitchContainerDb(id, translate, loading, locked, children.mapValues { it.value?.dd2() }, properties)
      is ContainerUniDb -> ContainerUniDb(id, loading, locked, child?.dd2(), properties)
      is ContainerBiDb -> ContainerBiDb(id, orientation, position, absoluteSize, collapsed, joined, loading, locked, children.mapValues { it.value?.dd2() }, properties)
      is ContainerSeqDb -> ContainerSeqDb(id, orientation, fill, alignment, joined, loading, locked, children.mapValues { it.value?.dd2() }, properties)
      is ContainerFreeFormDb -> ContainerFreeFormDb(id, loading, locked, showHeaders, children.mapValues { it.value?.dd2() }, properties)
      is WidgetDb -> WidgetDb(id, factoryId, nameUi, loading, locked, properties.dd(), fields)
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

class ContainerRootDb(
   id: UUID = UUID.randomUUID(),
   loading: Widget.LoadType = Widget.LoadType.AUTOMATIC,
   locked: Boolean = false,
   val child: ComponentDb? = null,
   properties: Map<String, Any?> = mapOf()
): ComponentDb(id, loading, locked, properties) {
   fun toUni() = ContainerUniDb(id, loading, locked, child, properties)
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

class ContainerUniDb(
   id: UUID = UUID.randomUUID(),
   loading: Widget.LoadType = Widget.LoadType.AUTOMATIC,
   locked: Boolean = false,
   val child: ComponentDb? = null,
   properties: Map<String, Any?> = mapOf()
): ComponentDb(id, loading, locked, properties) {
   override fun toDomain() = ContainerUni(this)
}

class ContainerBiDb(
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

class ContainerSeqDb(
   id: UUID = UUID.randomUUID(),
   val orientation: Orientation = Orientation.VERTICAL,
   val fill: Boolean = false,
   val alignment: Pos = Pos.CENTER,
   val joined: Boolean = false,
   loading: Widget.LoadType = Widget.LoadType.AUTOMATIC,
   locked: Boolean = false,
   val children: Map<Int, ComponentDb?> = mapOf(),
   properties: Map<String, Any?> = mapOf()
): ComponentDb(id, loading, locked, properties) {
   override fun toDomain() = ContainerSeq(this)
}

class ContainerFreeFormDb(
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
   val fields: Map<String, JsValue?> = mapOf()
): ComponentDb(id, loading, locked, properties) {
   override fun toDomain() = Widget(this)
}