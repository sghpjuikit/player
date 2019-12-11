package sp.it.pl.layout

import javafx.geometry.Orientation
import sp.it.pl.layout.container.BiContainer
import sp.it.pl.layout.container.FreeFormContainer
import sp.it.pl.layout.container.Layout
import sp.it.pl.layout.container.SwitchContainer
import sp.it.pl.layout.container.UniContainer
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.emptyWidgetFactory
import sp.it.pl.main.APP
import sp.it.pl.main.AppError
import sp.it.pl.main.ifErrorNotify
import sp.it.util.async.runIO
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.properties.PropVal
import sp.it.util.file.writeTextTry
import sp.it.util.functional.orNull
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
   f.writeTextTry(name).ifErrorNotify {
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
   }.orNull()?.toDomain()
}

abstract class ComponentDb(
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
   override fun toDomain() = SwitchContainer(this)
}

class UniContainerDb(
   id: UUID = UUID.randomUUID(),
   loading: Widget.LoadType = Widget.LoadType.AUTOMATIC,
   locked: Boolean = false,
   val child: ComponentDb? = null,
   properties: Map<String, Any?> = mapOf()
): ComponentDb(id, loading, locked, properties) {
   override fun toDomain() = UniContainer(this)
}

class BiContainerDb(
   id: UUID = UUID.randomUUID(),
   val orientation: Orientation = Orientation.VERTICAL,
   val position: Double = 0.5,
   val absoluteSize: Int = 0,
   val collapsed: Int = 0,
   loading: Widget.LoadType = Widget.LoadType.AUTOMATIC,
   locked: Boolean = false,
   val children: Map<Int, ComponentDb?> = mapOf(),
   properties: Map<String, Any?> = mapOf()
): ComponentDb(id, loading, locked, properties) {
   override fun toDomain() = BiContainer(this)
}

class FreeFormContainerDb(
   id: UUID = UUID.randomUUID(),
   loading: Widget.LoadType = Widget.LoadType.AUTOMATIC,
   locked: Boolean = false,
   val showHeaders: Boolean = true,
   val children: Map<Int, ComponentDb?> = mapOf(),
   properties: Map<String, Any?> = mapOf()
): ComponentDb(id, loading, locked, properties) {
   override fun toDomain() = FreeFormContainer(this)
}

class WidgetDb @JvmOverloads constructor(
   id: UUID = UUID.randomUUID(),
   val factoryId: String = emptyWidgetFactory.id(),
   val preferred: Boolean = false,
   val forbidUse: Boolean = false,
   val nameUi: String = emptyWidgetFactory.name(),
   loading: Widget.LoadType = Widget.LoadType.AUTOMATIC,
   locked: Boolean = false,
   properties: Map<String, Any?> = mapOf(),
   val settings: Map<String, PropVal?> = mapOf()
): ComponentDb(id, loading, locked, properties) {
   override fun toDomain() = Widget(this)
}